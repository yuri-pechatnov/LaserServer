package com.pechatnov;

import com.vaadin.server.Page;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;

import java.util.Arrays;

/**
 * Created by ura on 10.08.16.
 */
public class UsefulMethods {
    public static void notifyException(Exception e) {
        System.err.println("notifyException:" + e.getMessage());
        e.printStackTrace(System.err);
        new Notification("Exception!:",
                e.getMessage(),
                Notification.Type.ERROR_MESSAGE)
                .show(Page.getCurrent());
    }

    public static HorizontalLayout horisontalLayoutPair(AbstractComponent a, AbstractComponent b) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.addComponent(a);
        layout.addComponent(b);
        return layout;
    }

    public static void printCurrentStackTrace() {
        Arrays.asList(Thread.currentThread().getStackTrace()).forEach(System.err::println);
    }
}
