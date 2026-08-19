package com.mohamed.barcodelabel.render;

@FunctionalInterface
public interface LabelRenderer<T> {
    T render(RenderRequest request) throws RenderException;
}

