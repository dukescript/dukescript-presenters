package com.dukescript.presenters.androidapp;

/*
 * #%L
 * Android Test Application - a library from the "DukeScript Presenters" project.
 * 
 * Dukehoff GmbH designates this particular file as subject to the "Classpath"
 * exception as provided in the README.md file that accompanies this code.
 * %%
 * Copyright (C) 2015 - 2019 Dukehoff GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import com.dukescript.presenters.Android;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.Models;
import net.java.html.json.Property;

public class TestActivity extends Activity {
    private WebView view;
    private Executor presenter;
    private boolean clicked;
    private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new WebView(this);
        view.getSettings().setJavaScriptEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);
        button = new Button(this);
        button.setText("Run me");
        final LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                clicked = true;
                try {
                    click(view);
                } catch (IOException ex) {
                    Logger.getLogger(TestActivity.class.getName()).log(Level.SEVERE, null, ex);
                }
                button.setEnabled(false);
            }
        });
        ll.addView(button);
        ll.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        view.loadData("<h1 id='h1'>Press Run Button!</h1>", "text/html", "UTF-8");
        setContentView(ll);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    public void click(View v) throws IOException {
        final Executor p = (Executor) getPresenter("file:///android_asset/pages/index.html");
        p.execute(new Runnable() {
            @Override
            public void run() {
                ToDoModel m = new ToDoModel();
                Models.applyBindings(m);
            }
        });
    }

    public Executor getPresenter() {
        return getPresenter("file:///android_asset/pages/test.html");
    }

    private Executor getPresenter(String page) {
        if (presenter == null) {
            presenter = Android.configure("GPLv3", view, page, null);
        }
        return presenter;
    }

    public void changeName(final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(false);
                button.setText(name);
                button.setBackgroundColor(Color.BLACK);
                button.setTextColor(Color.WHITE);
            }
        });
    }

    public boolean error(final String msg) {
        final Thread toInterrupt[] = { Thread.currentThread() };
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setBackgroundColor(Color.YELLOW);
                button.setTextColor(Color.RED);
                button.setText(msg);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Thread t = toInterrupt[0];
                        if (t != null) {
                            t.interrupt();
                        }
                    }
                });
                button.setEnabled(true);
            }
        });
        try {
            Thread.sleep(9000);
            toInterrupt[0] = null;
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            return true;
        } finally {
            // clear the flag anyway
            Thread.currentThread().isInterrupted();
            changeName("");
        }
        return false;
    }

    static <E extends Exception> E raise(Class<E> type, Throwable t) throws E {
        throw (E)t;
    }

    @Model(className = "ToDoModel", properties = {
        @Property(name = "items", type = ItemModel.class, array = true),
        @Property(name = "text", type = String.class)
    })
    static final class ToDoController {
        @ComputedProperty static boolean nonempty(String text) {
            return text != null && !text.isEmpty();
        }
        @ComputedProperty static int pending(List<ItemModel> items) {
            if (items == null) return 0;
            int cnt = 0;
            for (ItemModel i : items) {
                if (!i.isDone()) {
                    cnt++;
                }
            }
            return cnt;
        }
        @ComputedProperty static int selected(List<ItemModel> items) {
            if (items == null) return 0;
            int cnt = 0;
            for (ItemModel i : items) {
                if (i.isDone()) {
                    cnt++;
                }
            }
            return cnt;
        }
        @Function static void add(ToDoModel model) {
            model.getItems().add(new ItemModel(false, model.getText()));
            model.setText("");
        }
        @Function static void archive(ToDoModel model) {
            Set<ItemModel> toRemove = new HashSet<ItemModel>();
            for (ItemModel item : model.getItems()) {
                if (item.isDone()) {
                    toRemove.add(item);
                }
            }
            model.getItems().removeAll(toRemove);
        }
    }
    @Model(className = "ItemModel", properties = {
        @Property(name = "done", type = boolean.class),
        @Property(name = "description", type = String.class),
    })
    static final class ItemController {
    }
}

