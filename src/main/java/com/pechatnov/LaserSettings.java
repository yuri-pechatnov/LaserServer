package com.pechatnov;

import com.vaadin.data.Property;
import com.vaadin.event.ShortcutAction;
import com.vaadin.shared.ui.slider.SliderOrientation;
import com.vaadin.ui.*;

/**
 * Created by ura on 10.08.16.
 */
public class LaserSettings extends VerticalLayout
        implements Draw.FlushTransformHandler {
    Laser laser;
    Draw draw;

    protected Slider intenseSlider, minIntenseSlider;

    protected void makeIntenseSetter() {
        /* intense Slider */ {
            intenseSlider = new Slider(1, 500);
            intenseSlider.setOrientation(SliderOrientation.HORIZONTAL);
            intenseSlider.setValue(laser.getIntensity().doubleValue());
            final Label intenseValue = new Label(laser.getIntensity().toString()), label = new Label("Laser intense and on/off manage");
            intenseValue.setSizeUndefined();
            label.setSizeUndefined();
            intenseSlider.addValueChangeListener(
                    event -> {
                        Integer value = intenseSlider.getValue().intValue();
                        if (value < laser.getMinIntensity())
                            minIntenseSlider.setValue(value.doubleValue());
                        laser.setIntensity(value);
                        intenseValue.setValue(value.toString());
                    });
            intenseSlider.setImmediate(true);
            intenseSlider.setWidth("300px");
            setDefaultComponentAlignment(Alignment.BOTTOM_LEFT);
            addComponent(label);
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setSpacing(true);
            horizontalLayout.addComponent(intenseSlider);
            horizontalLayout.addComponent(intenseValue);
            /*buttons */ {
                Button on = new Button("ON"), off = new Button("OFF");
                on.setClickShortcut(ShortcutAction.KeyCode.PAGE_UP);
                off.setClickShortcut(ShortcutAction.KeyCode.PAGE_DOWN);
                on.addClickListener(clickEvent -> {
                    try {
                        laser.on();
                    } catch (LaserConnectException e) {
                        UsefulMethods.notifyException(e);
                    }
                });
                horizontalLayout.addComponent(on);
                off.addClickListener(clickEvent -> {
                    try {
                        laser.off();
                    } catch (LaserConnectException e) {
                        UsefulMethods.notifyException(e);
                    }
                });
                horizontalLayout.addComponent(off);
            }
            addComponent(horizontalLayout);
        }
        /* minimial intense slider*/ {
            minIntenseSlider = new Slider(0, 500);
            minIntenseSlider.setOrientation(SliderOrientation.HORIZONTAL);
            minIntenseSlider.setValue(laser.getMinIntensity().doubleValue());
            final Label minIntenseValue = new Label(laser.getMinIntensity().toString()), minLabel = new Label("Minimal intensity (for gradual painting)");
            minIntenseValue.setSizeUndefined();
            minIntenseSlider.addValueChangeListener(event -> {
                Integer value = minIntenseSlider.getValue().intValue();
                if (value > laser.getIntensity())
                    intenseSlider.setValue(value.doubleValue());
                laser.setMinIntensity(value);
                minIntenseValue.setValue(value.toString());
            });
            minIntenseSlider.setImmediate(true);
            minIntenseSlider.setWidth("300px");
            addComponent(minLabel);
            addComponent(UsefulMethods.horisontalLayoutPair(minIntenseSlider, minIntenseValue));
        }
    }


    class CornerButton extends Button {
        public int tx, ty; // interface position
        CornerButton(String label, int tx_, int ty_) {
            super(label);
            setWidth("50px");
            setHeight("50px");
            addStyleName("f20pt");
            tx = tx_;
            ty = ty_;
            addClickListener(event -> {
                draw.fixCorner(tx, -ty);
                addStyleName("Green");
            });
        }
        CornerButton setClickShortcut(int keycode) {
            super.setClickShortcut(keycode);
            return this;
        }
    }

    protected CornerButton cornerButtons[];
    Button activateButton, flushButton;

    protected void makeCornersSetter() {
        Label label = new Label("DrawTransformation settings");
        addComponent(label);

        cornerButtons = new CornerButton[] {
                new CornerButton("↖", 0, 0).setClickShortcut(ShortcutAction.KeyCode.Q),
                new CornerButton("↗", 1, 0).setClickShortcut(ShortcutAction.KeyCode.W),
                new CornerButton("↙", 0, 1).setClickShortcut(ShortcutAction.KeyCode.A),
                new CornerButton("↘", 1, 1).setClickShortcut(ShortcutAction.KeyCode.S)
        };
        GridLayout grid = new GridLayout(3, 2);
        grid.addStyleName("gridlayout");
        for (CornerButton button : cornerButtons) {
            grid.addComponent(button, button.tx, button.ty);
        }
        activateButton = new Button("activate");
        flushButton = new Button("flush");
        activateButton.setHeight("50px");
        activateButton.setWidth("110px");
        flushButton.setHeight("50px");
        flushButton.setWidth("110px");
        activateButton.addClickListener(event -> {
            draw.applyCoordTransform();
            activateButton.addStyleName("Green");
            for (CornerButton button : cornerButtons) {
                button.removeStyleName("Green");
            }
        });
        flushButton.addClickListener(event -> {
            draw.flushTransform();
        });
        grid.addComponent(activateButton, 2, 0);
        grid.addComponent(flushButton, 2, 1);
        addComponent(grid);
    }


    LaserSettings(Laser laser_, Draw draw_) {
        laser = laser_;
        draw = draw_;
        draw.addFlushTransformHandler(this);
        setSpacing(true);
        makeIntenseSetter();
        makeCornersSetter();

    }

    @Override
    public void flushTransformHandler() {
        activateButton.removeStyleName("Green");
        for (CornerButton button : cornerButtons) {
            button.removeStyleName("Green");
        }
    }
}
