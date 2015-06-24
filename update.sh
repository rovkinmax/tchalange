#!/usr/local/bin/groovy


def static process(File file, Closure processText) {
    def text = file.text
    String newText = processText(text)
    //println newText
    file.write(newText)
}
def VERSION_NAME_PATTERN = /versionName\s+\"(.*)\"/
def VERSION_CODE_PATTERN = /versionCode\s+(\d+)/
def file = new File("main_lib/build.gradle")

process(file) {	        
	(it =~ VERSION_NAME_PATTERN).with {
		def (major,minor,fix) = getAt(0)[1].split("\\.").collect { it as int }			
		println (++fix)
		replaceAll("versionName \"${major}.${minor}.${fix}\"")	                    
	}
}

process(file){
	(it =~ VERSION_CODE_PATTERN).with {
		def versionCode = getAt(0)[1] as int
		println (++versionCode)
		
		replaceAll("versionCode ${versionCode}")	                    
	}
}

"git add .".execute().waitFor()
"git ci -m bump_version".execute().waitFor()




