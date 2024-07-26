package com.ji.adshelper.biling.listener;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;

/** @noinspection unused*/
public class IPurchaseLiveEvent<T> extends MutableLiveData<T> {
    private final HashMap<Integer, Boolean> pending = new HashMap<>();

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, new InnerObserver<T>(observer));
    }

    @Override
    public void postValue(T value) {
        for (int key : pending.keySet()) {
            pending.put(key, true);
        }
        super.postValue(value);
    }

    public void clear() {
        postValue(null);
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        super.removeObserver(observer);
        pending.remove(observer.hashCode());
    }

    public void notifyDataChanged() {
        postValue(getValue());
    }

    private class InnerObserver<R> implements Observer<R> {
        private final Observer<? super R> observer;

        public InnerObserver(Observer<? super R> observer) {
            pending.put(hashCode(), false);
            this.observer = observer;
        }

        @Override
        public void onChanged(R t) {
            boolean shouldNotify = pending.containsKey(hashCode()) && Boolean.TRUE.equals(pending.get(hashCode()));
            if (shouldNotify) {
                observer.onChanged(t);
                pending.put(hashCode(), false);
            }
        }
    }
}
