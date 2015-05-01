from __future__ import with_statement

import __builtin__
import __future__
import functools
import imp
import inspect
import json
from pathlib import Path
import optparse
import os
import os.path
import subprocess
import sys


# When build files are executed, the functions in this file tagged with
# @provide_for_build will be provided in the build file's local symbol table.
#
# When these functions are called from a build file, they will be passed
# a keyword parameter, build_env, which is a object with information about
# the environment of the build file which is currently being processed.
# It contains the following attributes:
#
# "dirname" - The directory containing the build file.
#
# "base_path" - The base path of the build file.

BUILD_FUNCTIONS = []


class BuildContextType(object):
    """
    Identifies the type of input file to the processor.
    """

    BUILD_FILE = 'build_file'
    INCLUDE = 'include'


class BuildFileContext(object):
    """
    The build context used when processing a build file.
    """

    type = BuildContextType.BUILD_FILE

    def __init__(self, base_path, dirname, allow_empty_globs):
        self.globals = {}
        self.includes = set()
        self.base_path = base_path
        self.dirname = dirname
        self.allow_empty_globs = allow_empty_globs
        self.rules = {}


class IncludeContext(object):
    """
    The build context used when processing an include.
    """

    type = BuildContextType.INCLUDE

    def __init__(self):
        self.globals = {}
        self.includes = set()


class LazyBuildEnvPartial(object):
    """Pairs a function with a build environment in which it will be executed.

    Note that while the function is specified via the constructor, the build
    environment must be assigned after construction, for the build environment
    currently being used.

    To call the function with its build environment, use the invoke() method of
    this class, which will forward the arguments from invoke() to the
    underlying function.
    """

    def __init__(self, func):
        self.func = func
        self.build_env = None

    def invoke(self, *args, **kwargs):
        """Invokes the bound function injecting 'build_env' into **kwargs."""
        updated_kwargs = kwargs.copy()
        updated_kwargs.update({'build_env': self.build_env})
        return self.func(*args, **updated_kwargs)


def provide_for_build(func):
    BUILD_FUNCTIONS.append(func)
    return func


def add_rule(rule, build_env):
    assert build_env.type == BuildContextType.BUILD_FILE, (
        "Cannot use `{}()` at the top-level of an included file."
        .format(rule['type']))

    # Include the base path of the BUILD file so the reader consuming this
    # JSON will know which BUILD file the rule came from.
    if 'name' not in rule:
        raise ValueError(
            'rules must contain the field \'name\'.  Found %s.' % rule)
    rule_name = rule['name']
    if rule_name in build_env.rules:
        raise ValueError('Duplicate rule definition found.  Found %s and %s' %
                         (rule, build_env.rules[rule_name]))
    rule['buck.base_path'] = build_env.base_path
    build_env.rules[rule_name] = rule


@provide_for_build
def glob(includes, excludes=[], include_dotfiles=False, build_env=None):
    assert build_env.type == BuildContextType.BUILD_FILE, (
        "Cannot use `glob()` at the top-level of an included file.")

    search_base = Path(build_env.dirname)
    return glob_internal(
        includes,
        excludes,
        include_dotfiles,
        build_env.allow_empty_globs,
        search_base)


def glob_internal(includes, excludes, include_dotfiles, allow_empty, search_base):
    # Ensure the user passes lists of strings rather than just a string.
    assert not isinstance(includes, basestring), \
        "The first argument to glob() must be a list of strings."
    assert not isinstance(excludes, basestring), \
        "The excludes argument must be a list of strings."

    def includes_iterator():
        for pattern in includes:
            for path in search_base.glob(pattern):
                # TODO(user): Handle hidden files on Windows.
                if path.is_file() and (include_dotfiles or not path.name.startswith('.')):
                    yield path.relative_to(search_base)

    def is_special(pat):
        return "*" in pat or "?" in pat or "[" in pat

    non_special_excludes = set()
    match_excludes = set()
    for pattern in excludes:
        if is_special(pattern):
            match_excludes.add(pattern)
        else:
            non_special_excludes.add(pattern)

    def exclusion(path):
        if path.as_posix() in non_special_excludes:
            return True
        for pattern in match_excludes:
            result = path.match(pattern, match_entire=True)
            if result:
                return True
        return False

    results = sorted(set([str(p) for p in includes_iterator() if not exclusion(p)]))
    assert allow_empty or results, (
        "glob(includes={includes}, excludes={excludes}, include_dotfiles={include_dotfiles}) " +
        "returned no results.  (allow_empty_globs is set to false in the Buck " +
        "configuration)").format(
            includes=includes,
            excludes=excludes,
            include_dotfiles=include_dotfiles)

    return results


@provide_for_build
def get_base_path(build_env=None):
    """Get the base path to the build file that was initially evaluated.

    This function is intended to be used from within a build defs file that
    likely contains macros that could be called from any build file.
    Such macros may need to know the base path of the file in which they
    are defining new build rules.

    Returns: a string, such as "java/com/facebook". Note there is no
             trailing slash. The return value will be "" if called from
             the build file in the root of the project.
    """
    assert build_env.type == BuildContextType.BUILD_FILE, (
        "Cannot use `get_base_path()` at the top-level of an included file.")
    return build_env.base_path


@provide_for_build
def add_deps(name, deps=[], build_env=None):
    assert build_env.type == BuildContextType.BUILD_FILE, (
        "Cannot use `add_deps()` at the top-level of an included file.")

    if name not in build_env.rules:
        raise ValueError(
            'Invoked \'add_deps\' on non-existent rule %s.' % name)

    rule = build_env.rules[name]
    if 'deps' not in rule:
        raise ValueError(
            'Invoked \'add_deps\' on rule %s that has no \'deps\' field'
            % name)
    rule['deps'] = rule['deps'] + deps


class BuildFileProcessor(object):

    def __init__(self, project_root, build_file_name, allow_empty_globs, implicit_includes=[]):
        self._cache = {}
        self._build_env_stack = []

        self._project_root = project_root
        self._build_file_name = build_file_name
        self._implicit_includes = implicit_includes
        self._allow_empty_globs = allow_empty_globs

        lazy_functions = {}
        for func in BUILD_FUNCTIONS:
            func_with_env = LazyBuildEnvPartial(func)
            lazy_functions[func.__name__] = func_with_env
        self._functions = lazy_functions

    def _merge_globals(self, mod, dst):
        """
        Copy the global definitions from one globals dict to another.

        Ignores special attributes and attributes starting with '_', which
        typically denote module-level private attributes.
        """

        hidden = set([
            'include_defs',
        ])

        keys = getattr(mod, '__all__', mod.__dict__.keys())

        for key in keys:
            if not key.startswith('_') and key not in hidden:
                dst[key] = mod.__dict__[key]

    def _update_functions(self, build_env):
        """
        Updates the build functions to use the given build context when called.
        """

        for function in self._functions.itervalues():
            function.build_env = build_env

    def install_builtins(self, namespace):
        """
        Installs the build functions, by their name, into the given namespace.
        """

        for name, function in self._functions.iteritems():
            namespace[name] = function.invoke

    def _get_include_path(self, name):
        """
        Resolve the given include def name to a full path.
        """

        # Find the path from the include def name.
        if not name.startswith('//'):
            raise ValueError(
                'include_defs argument "%s" must begin with //' % name)
        relative_path = name[2:]
        return os.path.join(self._project_root, name[2:])

    def _include_defs(self, name, implicit_includes=[]):
        """
        Pull the named include into the current caller's context.

        This method is meant to be installed into the globals of any files or
        includes that we process.
        """

        # Grab the current build context from the top of the stack.
        build_env = self._build_env_stack[-1]

        # Resolve the named include to its path and process it to get its
        # build context and module.
        path = self._get_include_path(name)
        inner_env, mod = self._process_include(
            path,
            implicit_includes=implicit_includes)

        # Look up the caller's stack frame and merge the include's globals
        # into it's symbol table.
        frame = inspect.currentframe()
        while frame.f_globals['__name__'] == __name__:
            frame = frame.f_back
        self._merge_globals(mod, frame.f_globals)

        # Pull in the include's accounting of its own referenced includes
        # into the current build context.
        build_env.includes.add(path)
        build_env.includes.update(inner_env.includes)

    def _push_build_env(self, build_env):
        """
        Set the given build context as the current context.
        """

        self._build_env_stack.append(build_env)
        self._update_functions(build_env)

    def _pop_build_env(self):
        """
        Restore the previous build context as the current context.
        """

        self._build_env_stack.pop()
        if self._build_env_stack:
            self._update_functions(self._build_env_stack[-1])

    def _process(self, build_env, path, implicit_includes=[]):
        """
        Process a build file or include at the given path.
        """

        # First check the cache.
        cached = self._cache.get(path)
        if cached is not None:
            return cached

        # Install the build context for this input as the current context.
        self._push_build_env(build_env)

        # The globals dict that this file will be executed under.
        default_globals = {}

        # Install the 'include_defs' function into our global object.
        default_globals['include_defs'] = functools.partial(
            self._include_defs,
            implicit_includes=implicit_includes)

        # If any implicit includes were specified, process them first.
        for include in implicit_includes:
            include_path = self._get_include_path(include)
            inner_env, mod = self._process_include(include_path)
            self._merge_globals(mod, default_globals)
            build_env.includes.add(include_path)
            build_env.includes.update(inner_env.includes)

        # Build a new module for the given file, using the default globals
        # created above.
        module = imp.new_module(path)
        module.__file__ = path
        module.__dict__.update(default_globals)

        with open(path) as f:
            contents = f.read()

        # Enable absolute imports.  This prevents the compiler from trying to
        # do a relative import first, and warning that this module doesn't
        # exist in sys.modules.
        future_features = __future__.absolute_import.compiler_flag
        code = compile(contents, path, 'exec', future_features, 1)
        exec(code, module.__dict__)

        # Restore the previous build context.
        self._pop_build_env()

        self._cache[path] = build_env, module
        return build_env, module

    def _process_include(self, path, implicit_includes=[]):
        """
        Process the include file at the given path.
        """

        build_env = IncludeContext()
        return self._process(
            build_env,
            path,
            implicit_includes=implicit_includes)

    def _process_build_file(self, path, implicit_includes=[]):
        """
        Process the build file at the given path.
        """

        # Create the build file context, including the base path and directory
        # name of the given path.
        relative_path_to_build_file = os.path.relpath(
            path, self._project_root).replace('\\', '/')
        len_suffix = -len('/' + self._build_file_name)
        base_path = relative_path_to_build_file[:len_suffix]
        dirname = os.path.dirname(path)
        build_env = BuildFileContext(base_path, dirname, self._allow_empty_globs)

        return self._process(
            build_env,
            path,
            implicit_includes=implicit_includes)

    def process(self, path):
        """
        Process a build file returning a dict of it's rules and includes.
        """
        build_env, mod = self._process_build_file(
            os.path.join(self._project_root, path),
            implicit_includes=self._implicit_includes)
        values = build_env.rules.values()
        values.append({"__includes": [path] + sorted(build_env.includes)})
        return values


def cygwin_adjusted_path(path):
    if sys.platform == 'cygwin':
        return subprocess.check_output(['cygpath', path]).rstrip()
    else:
        return path

# Inexplicably, this script appears to run faster when the arguments passed
# into it are absolute paths. However, we want the "buck.base_path" property
# of each rule to be printed out to be the base path of the build target that
# identifies the rule. That means that when parsing a BUILD file, we must know
# its path relative to the root of the project to produce the base path.
#
# To that end, the first argument to this script must be an absolute path to
# the project root.  It must be followed by one or more absolute paths to
# BUILD files under the project root.  If no paths to BUILD files are
# specified, then it will traverse the project root for BUILD files, excluding
# directories of generated files produced by Buck.
#
# All of the build rules that are parsed from the BUILD files will be printed
# to stdout by a JSON parser. That means that printing out other information
# for debugging purposes will likely break the JSON parsing, so be careful!


def main():
    # Our parent expects to read JSON from our stdout, so if anyone
    # uses print, buck will complain with a helpful "but I wanted an
    # array!" message and quit.  Redirect stdout to stderr so that
    # doesn't happen.  Actually dup2 the file handle so that writing
    # to file descriptor 1, os.system, and so on work as expected too.

    to_parent = os.fdopen(os.dup(sys.stdout.fileno()), 'a')
    os.dup2(sys.stderr.fileno(), sys.stdout.fileno())

    parser = optparse.OptionParser()
    parser.add_option(
        '--project_root',
        action='store',
        type='string',
        dest='project_root')
    parser.add_option(
        '--build_file_name',
        action='store',
        type='string',
        dest="build_file_name")
    parser.add_option(
        '--allow_empty_globs',
        action='store_true',
        dest='allow_empty_globs',
        help='Tells the parser not to raise an error when glob returns no results.')
    parser.add_option(
        '--include',
        action='append',
        dest='include')
    (options, args) = parser.parse_args()

    # Even though project_root is absolute path, it may not be concise. For
    # example, it might be like "C:\project\.\rule".
    #
    # Under cygwin, the project root will be invoked from buck as C:\path, but
    # the cygwin python uses UNIX-style paths. They can be converted using
    # cygpath, which is necessary because abspath will treat C:\path as a
    # relative path.
    options.project_root = cygwin_adjusted_path(options.project_root)
    project_root = os.path.abspath(options.project_root)

    buildFileProcessor = BuildFileProcessor(
        project_root,
        options.build_file_name,
        options.allow_empty_globs,
        implicit_includes=options.include or [])

    buildFileProcessor.install_builtins(__builtin__.__dict__)

    for build_file in args:
        build_file = cygwin_adjusted_path(build_file)
        values = buildFileProcessor.process(build_file)
        to_parent.write(json.dumps(values))
        to_parent.flush()

    # "for ... in sys.stdin" in Python 2.x hangs until stdin is closed.
    for build_file in iter(sys.stdin.readline, ''):
        build_file = cygwin_adjusted_path(build_file)
        values = buildFileProcessor.process(build_file.rstrip())
        to_parent.write(json.dumps(values))
        to_parent.flush()

    # Python tries to flush/close stdout when it quits, and if there's a dead
    # pipe on the other end, it will spit some warnings to stderr. This breaks
    # tests sometimes. Prevent that by explicitly catching the error.
    try:
        to_parent.close()
    except IOError:
        pass


@provide_for_build
def prebuilt_jar(name, binary_jar, deps=[], gwt_jar=None, javadoc_url=None, source_jar=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'prebuilt_jar',
    'name' : name,
    'binaryJar' : binary_jar,
    'deps' : deps,
    'gwtJar' : gwt_jar,
    'javadocUrl' : javadoc_url,
    'sourceJar' : source_jar,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_instrumentation_apk(name, apk, manifest, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'android_instrumentation_apk',
    'name' : name,
    'apk' : apk,
    'manifest' : manifest,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def gwt_binary(name, deps=[], draft_compile=None, experimental_args=[], local_workers=None, module_deps=[], modules=[], optimize=None, strict=None, style=None, vm_args=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'gwt_binary',
    'name' : name,
    'deps' : deps,
    'draftCompile' : draft_compile,
    'experimentalArgs' : experimental_args,
    'localWorkers' : local_workers,
    'moduleDeps' : module_deps,
    'modules' : modules,
    'optimize' : optimize,
    'strict' : strict,
    'style' : style,
    'vmArgs' : vm_args,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def d_binary(name, srcs, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'd_binary',
    'name' : name,
    'srcs' : srcs,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apple_asset_catalog(name, dirs, copy_to_bundles=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'apple_asset_catalog',
    'name' : name,
    'dirs' : dirs,
    'copyToBundles' : copy_to_bundles,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_build_config(name, package, values=None, values_file=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'android_build_config',
    'name' : name,
    'javaPackage' : package,
    'values' : values,
    'valuesFile' : values_file,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def java_library(name, annotation_processor_deps=[], annotation_processor_only=None, annotation_processor_params=[], annotation_processors=[], compiler=None, deps=[], exported_deps=[], extra_arguments=[], javac=None, javac_jar=None, postprocess_classes_commands=[], proguard_config=None, provided_deps=[], resources=[], resources_root=None, source=None, srcs=[], target=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'java_library',
    'name' : name,
    'annotationProcessorDeps' : annotation_processor_deps,
    'annotationProcessorOnly' : annotation_processor_only,
    'annotationProcessorParams' : annotation_processor_params,
    'annotationProcessors' : annotation_processors,
    'compiler' : compiler,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'extraArguments' : extra_arguments,
    'javac' : javac,
    'javacJar' : javac_jar,
    'postprocessClassesCommands' : postprocess_classes_commands,
    'proguardConfig' : proguard_config,
    'providedDeps' : provided_deps,
    'resources' : resources,
    'resourcesRoot' : resources_root,
    'source' : source,
    'srcs' : srcs,
    'target' : target,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def ios_postprocess_resources(name, cmd=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'ios_postprocess_resources',
    'name' : name,
    'cmd' : cmd,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apple_bundle(name, binary, extension, deps=[], files={}, headers={}, info_plist=None, tests=[], xcode_product_type=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'apple_bundle',
    'name' : name,
    'binary' : binary,
    'extension' : extension,
    'deps' : deps,
    'files' : files,
    'headers' : headers,
    'infoPlist' : info_plist,
    'tests' : tests,
    'xcodeProductType' : xcode_product_type,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def export_file(name, out=None, src=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'export_file',
    'name' : name,
    'out' : out,
    'src' : src,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apple_resource(name, dirs, files, variants={}, visibility=[], build_env=None):
  add_rule({
    'type' : 'apple_resource',
    'name' : name,
    'dirs' : dirs,
    'files' : files,
    'variants' : variants,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def ocaml_library(name, compiler_flags=[], deps=[], linker_flags=[], srcs=[], warnings_flags=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'ocaml_library',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'deps' : deps,
    'linkerFlags' : linker_flags,
    'srcs' : srcs,
    'warningsFlags' : warnings_flags,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def project_config(is_intellij_plugin=None, src_roots=[], src_target=None, test_roots=[], test_target=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'project_config',
    'name' : 'project_config',
    'isIntellijPlugin' : is_intellij_plugin,
    'srcRoots' : src_roots,
    'srcTarget' : src_target,
    'testRoots' : test_roots,
    'testTarget' : test_target,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def ocaml_binary(name, compiler_flags=[], deps=[], linker_flags=[], srcs=[], warnings_flags=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'ocaml_binary',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'deps' : deps,
    'linkerFlags' : linker_flags,
    'srcs' : srcs,
    'warningsFlags' : warnings_flags,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def buck_extension(name, srcs, deps=[], resources=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'buck_extension',
    'name' : name,
    'srcs' : srcs,
    'deps' : deps,
    'resources' : resources,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def prebuilt_ocaml_library(name, bytecode_lib=None, c_libs=[], deps=[], include_dir=None, lib_dir=None, lib_name=None, native_lib=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'prebuilt_ocaml_library',
    'name' : name,
    'bytecodeLib' : bytecode_lib,
    'cLibs' : c_libs,
    'deps' : deps,
    'includeDir' : include_dir,
    'libDir' : lib_dir,
    'libName' : lib_name,
    'nativeLib' : native_lib,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_binary(name, keystore, manifest, android_sdk_proguard_config=None, build_config_values=None, build_config_values_file=None, build_string_source_map=None, cpu_filters=[], deps=[], dex_compression=None, dex_reorder_data_dump_file=None, dex_reorder_tool_file=None, disable_pre_dex=None, exopackage=None, exopackage_modes=[], linear_alloc_hard_limit=None, locales=[], minimize_primary_dex_size=None, no_dx=[], optimization_passes=None, package_type=None, preprocess_java_classes_bash=None, preprocess_java_classes_deps=[], primary_dex_classes_file=None, primary_dex_patterns=[], primary_dex_scenario_file=None, primary_dex_scenario_overflow_allowed=None, proguard_config=None, reorder_classes_intra_dex=None, resource_compression=None, resource_filter=[], secondary_dex_head_classes_file=None, secondary_dex_tail_classes_file=None, skip_crunch_pngs=None, use_android_proguard_config_with_optimizations=None, use_linear_alloc_split_dex=None, use_split_dex=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'android_binary',
    'name' : name,
    'keystore' : keystore,
    'manifest' : manifest,
    'androidSdkProguardConfig' : android_sdk_proguard_config,
    'buildConfigValues' : build_config_values,
    'buildConfigValuesFile' : build_config_values_file,
    'buildStringSourceMap' : build_string_source_map,
    'cpuFilters' : cpu_filters,
    'deps' : deps,
    'dexCompression' : dex_compression,
    'dexReorderDataDumpFile' : dex_reorder_data_dump_file,
    'dexReorderToolFile' : dex_reorder_tool_file,
    'disablePreDex' : disable_pre_dex,
    'exopackage' : exopackage,
    'exopackageModes' : exopackage_modes,
    'linearAllocHardLimit' : linear_alloc_hard_limit,
    'locales' : locales,
    'minimizePrimaryDexSize' : minimize_primary_dex_size,
    'noDx' : no_dx,
    'optimizationPasses' : optimization_passes,
    'packageType' : package_type,
    'preprocessJavaClassesBash' : preprocess_java_classes_bash,
    'preprocessJavaClassesDeps' : preprocess_java_classes_deps,
    'primaryDexClassesFile' : primary_dex_classes_file,
    'primaryDexPatterns' : primary_dex_patterns,
    'primaryDexScenarioFile' : primary_dex_scenario_file,
    'primaryDexScenarioOverflowAllowed' : primary_dex_scenario_overflow_allowed,
    'proguardConfig' : proguard_config,
    'reorderClassesIntraDex' : reorder_classes_intra_dex,
    'resourceCompression' : resource_compression,
    'resourceFilter' : resource_filter,
    'secondaryDexHeadClassesFile' : secondary_dex_head_classes_file,
    'secondaryDexTailClassesFile' : secondary_dex_tail_classes_file,
    'skipCrunchPngs' : skip_crunch_pngs,
    'useAndroidProguardConfigWithOptimizations' : use_android_proguard_config_with_optimizations,
    'useLinearAllocSplitDex' : use_linear_alloc_split_dex,
    'useSplitDex' : use_split_dex,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def gen_parcelable(name, srcs, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'gen_parcelable',
    'name' : name,
    'srcs' : srcs,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_manifest(name, skeleton, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'android_manifest',
    'name' : name,
    'skeleton' : skeleton,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def ndk_library(name, deps=[], flags=[], is_asset=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'ndk_library',
    'name' : name,
    'deps' : deps,
    'flags' : flags,
    'isAsset' : is_asset,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_aar(name, manifest_skeleton, annotation_processor_deps=[], annotation_processor_only=None, annotation_processor_params=[], annotation_processors=[], compiler=None, deps=[], exported_deps=[], extra_arguments=[], javac=None, javac_jar=None, manifest=None, postprocess_classes_commands=[], proguard_config=None, provided_deps=[], resources=[], resources_root=None, source=None, srcs=[], target=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'android_aar',
    'name' : name,
    'manifestSkeleton' : manifest_skeleton,
    'annotationProcessorDeps' : annotation_processor_deps,
    'annotationProcessorOnly' : annotation_processor_only,
    'annotationProcessorParams' : annotation_processor_params,
    'annotationProcessors' : annotation_processors,
    'compiler' : compiler,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'extraArguments' : extra_arguments,
    'javac' : javac,
    'javacJar' : javac_jar,
    'manifest' : manifest,
    'postprocessClassesCommands' : postprocess_classes_commands,
    'proguardConfig' : proguard_config,
    'providedDeps' : provided_deps,
    'resources' : resources,
    'resourcesRoot' : resources_root,
    'source' : source,
    'srcs' : srcs,
    'target' : target,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apple_binary(name, compiler_flags=[], configs={}, deps=[], exported_deps=[], exported_headers=None, exported_linker_flags=[], exported_preprocessor_flags=[], extra_xcode_sources=[], frameworks=[], gid=None, header_path_prefix=None, headers=None, linker_flags=[], prefix_header=None, preprocessor_flags=[], srcs=[], tests=[], use_buck_header_maps=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'apple_binary',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'configs' : configs,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'exportedHeaders' : exported_headers,
    'exportedLinkerFlags' : exported_linker_flags,
    'exportedPreprocessorFlags' : exported_preprocessor_flags,
    'extraXcodeSources' : extra_xcode_sources,
    'frameworks' : frameworks,
    'gid' : gid,
    'headerPathPrefix' : header_path_prefix,
    'headers' : headers,
    'linkerFlags' : linker_flags,
    'prefixHeader' : prefix_header,
    'preprocessorFlags' : preprocessor_flags,
    'srcs' : srcs,
    'tests' : tests,
    'useBuckHeaderMaps' : use_buck_header_maps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def cxx_python_extension(name, base_module=None, compiler_flags=[], deps=[], framework_search_paths=[], header_namespace=None, headers=None, lang_preprocessor_flags={}, lex_srcs=[], linker_flags=[], platform_compiler_flags=[], platform_linker_flags=[], platform_preprocessor_flags=[], prefix_headers=[], preprocessor_flags=[], srcs=None, tests=[], yacc_srcs=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'cxx_python_extension',
    'name' : name,
    'baseModule' : base_module,
    'compilerFlags' : compiler_flags,
    'deps' : deps,
    'frameworkSearchPaths' : framework_search_paths,
    'headerNamespace' : header_namespace,
    'headers' : headers,
    'langPreprocessorFlags' : lang_preprocessor_flags,
    'lexSrcs' : lex_srcs,
    'linkerFlags' : linker_flags,
    'platformCompilerFlags' : platform_compiler_flags,
    'platformLinkerFlags' : platform_linker_flags,
    'platformPreprocessorFlags' : platform_preprocessor_flags,
    'prefixHeaders' : prefix_headers,
    'preprocessorFlags' : preprocessor_flags,
    'srcs' : srcs,
    'tests' : tests,
    'yaccSrcs' : yacc_srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def robolectric_test(name, annotation_processor_deps=[], annotation_processor_only=None, annotation_processor_params=[], annotation_processors=[], compiler=None, contacts=[], deps=[], exported_deps=[], extra_arguments=[], javac=None, javac_jar=None, labels=[], postprocess_classes_commands=[], proguard_config=None, provided_deps=[], resources=[], resources_root=None, source=None, source_under_test=[], srcs=[], target=None, test_type=None, vm_args=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'robolectric_test',
    'name' : name,
    'annotationProcessorDeps' : annotation_processor_deps,
    'annotationProcessorOnly' : annotation_processor_only,
    'annotationProcessorParams' : annotation_processor_params,
    'annotationProcessors' : annotation_processors,
    'compiler' : compiler,
    'contacts' : contacts,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'extraArguments' : extra_arguments,
    'javac' : javac,
    'javacJar' : javac_jar,
    'labels' : labels,
    'postprocessClassesCommands' : postprocess_classes_commands,
    'proguardConfig' : proguard_config,
    'providedDeps' : provided_deps,
    'resources' : resources,
    'resourcesRoot' : resources_root,
    'source' : source,
    'sourceUnderTest' : source_under_test,
    'srcs' : srcs,
    'target' : target,
    'testType' : test_type,
    'vmArgs' : vm_args,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def thrift_library(name, srcs, cpp2_deps=[], cpp2_options=[], cpp_deps=[], cpp_exported_headers=None, cpp_header_namespace=None, cpp_options=[], cpp_srcs=None, deps=[], flags=[], java_options=[], py_base_module=None, py_options=[], py_twisted_base_module=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'thrift_library',
    'name' : name,
    'srcs' : srcs,
    'cpp2Deps' : cpp2_deps,
    'cpp2Options' : cpp2_options,
    'cppDeps' : cpp_deps,
    'cppExportedHeaders' : cpp_exported_headers,
    'cppHeaderNamespace' : cpp_header_namespace,
    'cppOptions' : cpp_options,
    'cppSrcs' : cpp_srcs,
    'deps' : deps,
    'flags' : flags,
    'javaOptions' : java_options,
    'pyBaseModule' : py_base_module,
    'pyOptions' : py_options,
    'pyTwistedBaseModule' : py_twisted_base_module,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def python_binary(name, base_module=None, deps=[], main=None, main_module=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'python_binary',
    'name' : name,
    'baseModule' : base_module,
    'deps' : deps,
    'main' : main,
    'mainModule' : main_module,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def d_test(name, deps, srcs, contacts=[], labels=[], source_under_test=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'd_test',
    'name' : name,
    'deps' : deps,
    'srcs' : srcs,
    'contacts' : contacts,
    'labels' : labels,
    'sourceUnderTest' : source_under_test,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_prebuilt_aar(name, aar, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'android_prebuilt_aar',
    'name' : name,
    'aar' : aar,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def python_library(name, base_module=None, deps=[], resources=None, srcs=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'python_library',
    'name' : name,
    'baseModule' : base_module,
    'deps' : deps,
    'resources' : resources,
    'srcs' : srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def gen_aidl(name, aidl, import_path, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'gen_aidl',
    'name' : name,
    'aidl' : aidl,
    'importPath' : import_path,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def cxx_binary(name, compiler_flags=[], deps=[], framework_search_paths=[], header_namespace=None, headers=None, lang_preprocessor_flags={}, lex_srcs=[], linker_flags=[], platform_compiler_flags=[], platform_linker_flags=[], platform_preprocessor_flags=[], prefix_headers=[], preprocessor_flags=[], srcs=None, tests=[], yacc_srcs=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'cxx_binary',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'deps' : deps,
    'frameworkSearchPaths' : framework_search_paths,
    'headerNamespace' : header_namespace,
    'headers' : headers,
    'langPreprocessorFlags' : lang_preprocessor_flags,
    'lexSrcs' : lex_srcs,
    'linkerFlags' : linker_flags,
    'platformCompilerFlags' : platform_compiler_flags,
    'platformLinkerFlags' : platform_linker_flags,
    'platformPreprocessorFlags' : platform_preprocessor_flags,
    'prefixHeaders' : prefix_headers,
    'preprocessorFlags' : preprocessor_flags,
    'srcs' : srcs,
    'tests' : tests,
    'yaccSrcs' : yacc_srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def genrule(name, out, bash=None, cmd=None, cmd_exe=None, deps=[], srcs=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'genrule',
    'name' : name,
    'out' : out,
    'bash' : bash,
    'cmd' : cmd,
    'cmdExe' : cmd_exe,
    'deps' : deps,
    'srcs' : srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def prebuilt_cxx_library(name, deps=[], exported_linker_flags=[], exported_platform_linker_flags=[], header_only=None, include_dirs=[], lib_dir=None, lib_name=None, link_whole=None, provided=None, soname=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'prebuilt_cxx_library',
    'name' : name,
    'deps' : deps,
    'exportedLinkerFlags' : exported_linker_flags,
    'exportedPlatformLinkerFlags' : exported_platform_linker_flags,
    'headerOnly' : header_only,
    'includeDirs' : include_dirs,
    'libDir' : lib_dir,
    'libName' : lib_name,
    'linkWhole' : link_whole,
    'provided' : provided,
    'soname' : soname,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def d_library(name, srcs, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'd_library',
    'name' : name,
    'srcs' : srcs,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def core_data_model(name, path, visibility=[], build_env=None):
  add_rule({
    'type' : 'core_data_model',
    'name' : name,
    'path' : path,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def keystore(name, properties, store, deps=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'keystore',
    'name' : name,
    'properties' : properties,
    'store' : store,
    'deps' : deps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def prebuilt_native_library(name, native_libs, deps=[], is_asset=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'prebuilt_native_library',
    'name' : name,
    'nativeLibs' : native_libs,
    'deps' : deps,
    'isAsset' : is_asset,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def python_test(name, base_module=None, contacts=[], deps=[], labels=[], resources=None, source_under_test=[], srcs=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'python_test',
    'name' : name,
    'baseModule' : base_module,
    'contacts' : contacts,
    'deps' : deps,
    'labels' : labels,
    'resources' : resources,
    'sourceUnderTest' : source_under_test,
    'srcs' : srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def xcode_workspace_config(name, action_config_names={}, extra_schemes={}, extra_targets=[], extra_tests=[], is_remote_runnable=None, src_target=None, workspace_name=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'xcode_workspace_config',
    'name' : name,
    'actionConfigNames' : action_config_names,
    'extraSchemes' : extra_schemes,
    'extraTargets' : extra_targets,
    'extraTests' : extra_tests,
    'isRemoteRunnable' : is_remote_runnable,
    'srcTarget' : src_target,
    'workspaceName' : workspace_name,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apk_genrule(name, apk, out, bash=None, cmd=None, cmd_exe=None, deps=[], srcs=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'apk_genrule',
    'name' : name,
    'apk' : apk,
    'out' : out,
    'bash' : bash,
    'cmd' : cmd,
    'cmdExe' : cmd_exe,
    'deps' : deps,
    'srcs' : srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apple_library(name, compiler_flags=[], configs={}, deps=[], exported_deps=[], exported_headers=None, exported_linker_flags=[], exported_preprocessor_flags=[], extra_xcode_sources=[], frameworks=[], gid=None, header_path_prefix=None, headers=None, linker_flags=[], prefix_header=None, preprocessor_flags=[], srcs=[], tests=[], use_buck_header_maps=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'apple_library',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'configs' : configs,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'exportedHeaders' : exported_headers,
    'exportedLinkerFlags' : exported_linker_flags,
    'exportedPreprocessorFlags' : exported_preprocessor_flags,
    'extraXcodeSources' : extra_xcode_sources,
    'frameworks' : frameworks,
    'gid' : gid,
    'headerPathPrefix' : header_path_prefix,
    'headers' : headers,
    'linkerFlags' : linker_flags,
    'prefixHeader' : prefix_header,
    'preprocessorFlags' : preprocessor_flags,
    'srcs' : srcs,
    'tests' : tests,
    'useBuckHeaderMaps' : use_buck_header_maps,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_resource(name, assets=None, deps=[], has_whitelisted_strings=None, manifest=None, package=None, res=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'android_resource',
    'name' : name,
    'assets' : assets,
    'deps' : deps,
    'hasWhitelistedStrings' : has_whitelisted_strings,
    'manifest' : manifest,
    'rDotJavaPackage' : package,
    'res' : res,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def sh_test(name, test, args=[], deps=[], labels=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'sh_test',
    'name' : name,
    'test' : test,
    'args' : args,
    'deps' : deps,
    'labels' : labels,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def java_binary(name, blacklist=[], deps=[], main_class=None, manifest_file=None, merge_manifests=None, meta_inf_directory=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'java_binary',
    'name' : name,
    'blacklist' : blacklist,
    'deps' : deps,
    'mainClass' : main_class,
    'manifestFile' : manifest_file,
    'mergeManifests' : merge_manifests,
    'metaInfDirectory' : meta_inf_directory,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def java_test(name, annotation_processor_deps=[], annotation_processor_only=None, annotation_processor_params=[], annotation_processors=[], compiler=None, contacts=[], deps=[], exported_deps=[], extra_arguments=[], javac=None, javac_jar=None, labels=[], postprocess_classes_commands=[], proguard_config=None, provided_deps=[], resources=[], resources_root=None, source=None, source_under_test=[], srcs=[], target=None, test_type=None, vm_args=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'java_test',
    'name' : name,
    'annotationProcessorDeps' : annotation_processor_deps,
    'annotationProcessorOnly' : annotation_processor_only,
    'annotationProcessorParams' : annotation_processor_params,
    'annotationProcessors' : annotation_processors,
    'compiler' : compiler,
    'contacts' : contacts,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'extraArguments' : extra_arguments,
    'javac' : javac,
    'javacJar' : javac_jar,
    'labels' : labels,
    'postprocessClassesCommands' : postprocess_classes_commands,
    'proguardConfig' : proguard_config,
    'providedDeps' : provided_deps,
    'resources' : resources,
    'resourcesRoot' : resources_root,
    'source' : source,
    'sourceUnderTest' : source_under_test,
    'srcs' : srcs,
    'target' : target,
    'testType' : test_type,
    'vmArgs' : vm_args,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def cxx_library(name, compiler_flags=[], deps=[], exported_headers=None, exported_lang_preprocessor_flags={}, exported_linker_flags=[], exported_platform_linker_flags=[], exported_platform_preprocessor_flags=[], exported_preprocessor_flags=[], force_static=None, framework_search_paths=[], header_namespace=None, headers=None, lang_preprocessor_flags={}, lex_srcs=[], link_whole=None, linker_flags=[], platform_compiler_flags=[], platform_linker_flags=[], platform_preprocessor_flags=[], prefix_headers=[], preprocessor_flags=[], soname=None, srcs=None, tests=[], yacc_srcs=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'cxx_library',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'deps' : deps,
    'exportedHeaders' : exported_headers,
    'exportedLangPreprocessorFlags' : exported_lang_preprocessor_flags,
    'exportedLinkerFlags' : exported_linker_flags,
    'exportedPlatformLinkerFlags' : exported_platform_linker_flags,
    'exportedPlatformPreprocessorFlags' : exported_platform_preprocessor_flags,
    'exportedPreprocessorFlags' : exported_preprocessor_flags,
    'forceStatic' : force_static,
    'frameworkSearchPaths' : framework_search_paths,
    'headerNamespace' : header_namespace,
    'headers' : headers,
    'langPreprocessorFlags' : lang_preprocessor_flags,
    'lexSrcs' : lex_srcs,
    'linkWhole' : link_whole,
    'linkerFlags' : linker_flags,
    'platformCompilerFlags' : platform_compiler_flags,
    'platformLinkerFlags' : platform_linker_flags,
    'platformPreprocessorFlags' : platform_preprocessor_flags,
    'prefixHeaders' : prefix_headers,
    'preprocessorFlags' : preprocessor_flags,
    'soname' : soname,
    'srcs' : srcs,
    'tests' : tests,
    'yaccSrcs' : yacc_srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def sh_binary(name, main, deps=[], resources=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'sh_binary',
    'name' : name,
    'main' : main,
    'deps' : deps,
    'resources' : resources,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def remote_file(name, sha1, url, out=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'remote_file',
    'name' : name,
    'sha1' : sha1,
    'url' : url,
    'out' : out,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def android_library(name, annotation_processor_deps=[], annotation_processor_only=None, annotation_processor_params=[], annotation_processors=[], compiler=None, deps=[], exported_deps=[], extra_arguments=[], javac=None, javac_jar=None, manifest=None, postprocess_classes_commands=[], proguard_config=None, provided_deps=[], resources=[], resources_root=None, source=None, srcs=[], target=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'android_library',
    'name' : name,
    'annotationProcessorDeps' : annotation_processor_deps,
    'annotationProcessorOnly' : annotation_processor_only,
    'annotationProcessorParams' : annotation_processor_params,
    'annotationProcessors' : annotation_processors,
    'compiler' : compiler,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'extraArguments' : extra_arguments,
    'javac' : javac,
    'javacJar' : javac_jar,
    'manifest' : manifest,
    'postprocessClassesCommands' : postprocess_classes_commands,
    'proguardConfig' : proguard_config,
    'providedDeps' : provided_deps,
    'resources' : resources,
    'resourcesRoot' : resources_root,
    'source' : source,
    'srcs' : srcs,
    'target' : target,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def cxx_test(name, compiler_flags=[], contacts=[], deps=[], framework=None, framework_search_paths=[], header_namespace=None, headers=None, labels=[], lang_preprocessor_flags={}, lex_srcs=[], linker_flags=[], platform_compiler_flags=[], platform_linker_flags=[], platform_preprocessor_flags=[], prefix_headers=[], preprocessor_flags=[], source_under_test=[], srcs=None, tests=[], yacc_srcs=[], visibility=[], build_env=None):
  add_rule({
    'type' : 'cxx_test',
    'name' : name,
    'compilerFlags' : compiler_flags,
    'contacts' : contacts,
    'deps' : deps,
    'framework' : framework,
    'frameworkSearchPaths' : framework_search_paths,
    'headerNamespace' : header_namespace,
    'headers' : headers,
    'labels' : labels,
    'langPreprocessorFlags' : lang_preprocessor_flags,
    'lexSrcs' : lex_srcs,
    'linkerFlags' : linker_flags,
    'platformCompilerFlags' : platform_compiler_flags,
    'platformLinkerFlags' : platform_linker_flags,
    'platformPreprocessorFlags' : platform_preprocessor_flags,
    'prefixHeaders' : prefix_headers,
    'preprocessorFlags' : preprocessor_flags,
    'sourceUnderTest' : source_under_test,
    'srcs' : srcs,
    'tests' : tests,
    'yaccSrcs' : yacc_srcs,
    'visibility' : visibility,
  }, build_env)


@provide_for_build
def apple_test(name, extension, can_group=None, compiler_flags=[], configs={}, contacts=[], deps=[], exported_deps=[], exported_headers=None, exported_linker_flags=[], exported_preprocessor_flags=[], extra_xcode_sources=[], frameworks=[], gid=None, header_path_prefix=None, headers=None, info_plist=None, labels=[], linker_flags=[], prefix_header=None, preprocessor_flags=[], srcs=[], tests=[], use_buck_header_maps=None, xcode_product_type=None, visibility=[], build_env=None):
  add_rule({
    'type' : 'apple_test',
    'name' : name,
    'extension' : extension,
    'canGroup' : can_group,
    'compilerFlags' : compiler_flags,
    'configs' : configs,
    'contacts' : contacts,
    'deps' : deps,
    'exportedDeps' : exported_deps,
    'exportedHeaders' : exported_headers,
    'exportedLinkerFlags' : exported_linker_flags,
    'exportedPreprocessorFlags' : exported_preprocessor_flags,
    'extraXcodeSources' : extra_xcode_sources,
    'frameworks' : frameworks,
    'gid' : gid,
    'headerPathPrefix' : header_path_prefix,
    'headers' : headers,
    'infoPlist' : info_plist,
    'labels' : labels,
    'linkerFlags' : linker_flags,
    'prefixHeader' : prefix_header,
    'preprocessorFlags' : preprocessor_flags,
    'srcs' : srcs,
    'tests' : tests,
    'useBuckHeaderMaps' : use_buck_header_maps,
    'xcodeProductType' : xcode_product_type,
    'visibility' : visibility,
  }, build_env)


if __name__ == '__main__':
  try:
    main()
  except KeyboardInterrupt:
    print >> sys.stderr, 'Killed by User'
