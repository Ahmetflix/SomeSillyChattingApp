package xyz.ahmetflix.chattingserver;

import com.google.common.util.concurrent.ListenableFuture;

public interface IAsyncTaskHandler {
    ListenableFuture<Object> postToMainThread(Runnable var1);

    boolean isMainThread();
}
