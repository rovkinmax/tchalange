package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.glide.Stub;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RxGlide {

    public static final String TELEGRAM_FILE = "telegram.file.";

    private Context ctx;

    @Inject
    public RxGlide(Context ctx) {
        this.ctx = ctx;
    }


    private static final RxGlide.StubAware<TdApi.GroupChat> STUB_AWARE_GROUP_CHAT = new StubAware<TdApi.GroupChat>() {
        @Override
        public String needStub(TdApi.GroupChat o) {
            TdApi.GroupChat chat = o;
            String title = chat.title;
            if (title.length() > 0) {
                return String.valueOf(
                        Character.toUpperCase(title.charAt(0)));
            }
            return "";
        }
    };

    private static final RxGlide.StubAware<TdApi.User> STUB_AWARE_USER = new StubAware<TdApi.User>() {
        @Override
        public String needStub(TdApi.User o) {
            TdApi.User user = o;
            StringBuilder sb = new StringBuilder();
            if (user.firstName.length() > 0) {
                sb.append(
                        Character.toUpperCase(
                                user.firstName.charAt(0)));
            }
            if (user.lastName.length() > 0) {
                sb.append(
                        Character.toUpperCase(
                                user.lastName.charAt(0)));
            }
            return sb.toString();
        }
    };

    public DrawableRequestBuilder loadAvatarForUser(TdApi.User u, int size) {
        TdApi.File file = u.photoSmall;
        if (file instanceof TdApi.FileEmpty) {
            boolean stub = ((TdApi.FileEmpty) file).id == 0;
            if (stub) {
                return loadStub(u, size);
            }
        }
        return Glide.with(ctx)
                .load(file)
                .override(size, size);
    }

    private DrawableRequestBuilder loadStub(TdApi.User u, int size) {
        return Glide.with(ctx)
                .load(new Stub(STUB_AWARE_USER.needStub(u), u.id))
                .override(size, size);
    }

    private DrawableRequestBuilder loadStub(TdApi.GroupChatInfo info, int size) {
        return Glide.with(ctx)
                .load(new Stub(STUB_AWARE_GROUP_CHAT.needStub(info.groupChat), info.groupChat.id))
                .override(size, size);
    }

    public DrawableRequestBuilder loadAvatarForChat(TdApi.Chat chat, int size) {
        if (chat.type instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) chat.type).user;
            return loadAvatarForUser(user, size);
        } else {
            return loadAvatarForGroup(chat, size);
        }
    }

    private DrawableRequestBuilder loadAvatarForGroup(TdApi.Chat chat, int size) {
        TdApi.GroupChatInfo info = (TdApi.GroupChatInfo) chat.type;
        TdApi.File file = info.groupChat.photoSmall;
        if (file instanceof TdApi.FileEmpty) {
            boolean stub = ((TdApi.FileEmpty) file).id == 0;
            if (stub) {
                return loadStub(info, size);
            }
        }
        return Glide.with(ctx)
                .load(file)
                .override(size, size);
    }



    public DrawableTypeRequest<TdApi.File> loadPhoto(TdApi.File f) {
        return Glide.with(ctx)
                .load(f);

    }


    public static final int TIMEOUT = 30000;


    public RequestManager getPicasso() {
        return Glide.with(ctx);
    }

    //    public void cancelRequest(AvatarView avatarView) {
//        Glide.with(ctx).c
//    }
    //    private class RXRequestHandler extends RequestHandler {
    //
    //        public static final String URI_SCHEME = "telegram";
    //        public static final String URI_PARAM_ID = "id";
    //        public static final String URI_PARAM_STUB = "stub";
    //
    //        public static final String URI_PARAM_WEBP = "webp";
    //        private final int stubTextSize;

    //        public RXRequestHandler(final Context ctx) {
    //
    //        }

    //        @Override
    //        public boolean canHandleRequest(Request data) {
    //            return URI_SCHEME.equals(data.uri.getScheme());
    //        }

    //        @Override
    //        public Result load(Request request, int networkPolicy) throws IOException {
    //            String strId = request.uri.getQueryParameter(URI_PARAM_ID);
    //            if (strId == null) {
    //                throw new IllegalArgumentException("wrong uri");
    //            }
    //            int id = Integer.parseInt(strId);
    //
    //            String stub = request.uri.getQueryParameter(URI_PARAM_STUB);
    //            if (stub == null) {
    //                String webp = request.uri.getQueryParameter(URI_PARAM_WEBP);
    //                return loadFileEmpty(id);
    //            } else {
    //                return loadStub(request, id, stub);
    //            }
    //        }

    //        private Result loadStub(Request request, int id, String stub) {
    //            int size = request.targetWidth;
    //            int colorFor = AvatarStubColors.getColorFor(id);
    //
    //            TextPaint stubTextPaint = textPaints.get();
    //            stubTextPaint.setColor(Color.WHITE);//todo!!!!
    //            Paint paint = paints.get();
    //            paint.setColor(colorFor);
    //
    //            StaticLayout staticLayout = new StaticLayout(stub, stubTextPaint, size, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
    //            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);//todo mb less?
    //            Canvas canvas = new Canvas(bmp);
    //            RectF r = rects.get();
    //            r.set(0, 0, size, size);
    //            canvas.drawOval(r, paint);
    //            int height = staticLayout.getHeight();
    //            int p = (size - height) / 2;
    //            canvas.save();
    //            canvas.translate(0, p);
    //            staticLayout.draw(canvas);
    //            canvas.restore();
    //            return new Result(bmp, Picasso.LoadedFrom.NETWORK);
    //        }

    //        private Result loadFileEmpty(final int id) throws IOException {
    //            TdApi.UpdateFile first;
    //            try {
    //                Observable<TdApi.UpdateFile> specificFileUpdate = client.filesUpdates()
    //                        .filter(new Func1<TdApi.UpdateFile, Boolean>() {
    //                            @Override
    //                            public Boolean call(TdApi.UpdateFile u) {
    //                                return u.fileId == id;
    //                            }
    //                        })
    //                        .first();
    //                client.sendSilently(new TdApi.DownloadFile(id));
    //                first = specificFileUpdate.toBlocking()
    //                        .toFuture()
    //                        .get(TIMEOUT, TimeUnit.MILLISECONDS);
    ////                https://code.google.com/p/webp/issues/detail?id=147
    ////                WebP support for transparent files was added in Android JB-MR2 (4.2) onwards.
    ////                if (webp  && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
    ////                    return supportDecode(first);
    ////                }
    //                FileInputStream res = new FileInputStream(new File(first.path));
    //                return new Result(res, Picasso.LoadedFrom.NETWORK);
    //            } catch (InterruptedException e) {
    //                Thread.currentThread().interrupt();
    //                throw new IOException();
    //            } catch (ExecutionException | TimeoutException e) {
    //                throw new IOException();
    //            }
    //        }

    //        private Result supportDecode(TdApi.UpdateFile first) throws IOException {
    //
    //
    //            RandomAccessFile f = new RandomAccessFile(first.path, "r");
    //            byte[] b = new byte[(int)f.length()];
    //            f.readFully(b);//todo pretty sure libwebp can decode image from file. so do no
    //
    //            Bitmap bmp = WebPFactory.nativeDecodeByteArray(b, null);
    //            return new Result(bmp, Picasso.LoadedFrom.NETWORK);
    //
    //        }
    //    }

    //    private static class CircleTransformation implements Transformation {
    //        //todo thread locals for
    //        @Override
    //        public Bitmap transform(Bitmap source) {
    //            int width = source.getWidth();
    //            int height = source.getHeight();
    //            Bitmap transformed = Bitmap.createBitmap(width, height, source.getConfig());
    //            Canvas canvas = new Canvas(transformed);
    //            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    //            p.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
    //            canvas.drawCircle(width / 2, height / 2, width / 2, p);
    //            source.recycle();
    //            return transformed;
    //        }
    //
    //        @Override
    //        public String key() {
    //            return "rounded";
    //        }
    //    }

    public interface StubAware<T> {
        String needStub(T o);
    }

    public static String id(TdApi.FileLocal f) {
        return TELEGRAM_FILE + f.id;
    }

    public static String id(TdApi.FileEmpty f) {
        return TELEGRAM_FILE + f.id;
    }
}
