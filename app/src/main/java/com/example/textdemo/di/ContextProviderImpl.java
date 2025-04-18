package com.example.textdemo.di;

import android.content.Context;

import com.example.textdemo.utils.common.ContextProvider;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class ContextProviderImpl implements ContextProvider {
    private final Context context;

    @Inject
    public ContextProviderImpl(@ApplicationContext Context context) {
        this.context = context;
    }


    @Override
    public Context getContext() {
        return context;
    }
}
