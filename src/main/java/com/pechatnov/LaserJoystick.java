package com.pechatnov;

import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.*;

import java.util.Arrays;

/**
 * Created by ura on 10.08.16.
 */
public class LaserJoystick extends HorizontalLayout {
    final Laser laser;
    final Draw draw;


    class NumSetter extends OptionGroup {
        protected Integer[] values;

        protected String doMap(Integer x) {
            return x.toString() + "px";
        }

        public Integer ask() {
            for (Integer x : values) {
                if (isSelected(doMap(x)))
                    return x;
            }
            return -1;
        }

        NumSetter(String label, Integer... valuesArg) {
            super(label);
            values = valuesArg;
            addItems(Arrays.asList(values).stream().map(this::doMap).toArray());
            select(doMap(values[0]));
        }
    }

    NumSetter stepLenSetter;
    Label coordLabel;
    Integer stepLen;

    protected GridLayout makeDirButtons() {
        class DirButton extends Button {
            public int tx, ty; // interface position
            protected int dirx, diry; // direction
            DirButton(String label, int dirx_, int diry_) {
                super(label);
                setWidth("50px");
                setHeight("50px");
                addStyleName("f20pt");
                dirx = dirx_;
                diry = diry_;
                tx = 1 + dirx;
                ty = 1 - diry;
                if (dirx == 0 &&  diry == 0) {
                    addClickListener(new Button.ClickListener() {
                        @Override
                        public void buttonClick(ClickEvent event) {
                            try {
                                laser.makeDot();
                                coordLabel.setValue(draw.getCoordStr());
                            } catch (LaserConnectException e) {
                                UsefulMethods.notifyException(e);
                            }
                        }
                    });
                }
                else {
                    addClickListener(new Button.ClickListener() {
                        @Override
                        public void buttonClick(ClickEvent event) {
                            try {
                                laser.move(dirx * stepLen, diry * stepLen);
                                coordLabel.setValue(draw.getCoordStr());
                            } catch (LaserConnectException e) {
                                UsefulMethods.notifyException(e);
                            }
                        }
                    });
                }
            }
            DirButton setClickShortcut(int keycode) {
                super.setClickShortcut(keycode);
                return this;
            }
        }
        GridLayout grid = new GridLayout(3, 3);
        grid.addStyleName("example-gridlayout");

        DirButton buttons[] = {
                // new DirButton("⬁", -1, +1),
                new DirButton("▲",  0, +1).setClickShortcut(ShortcutAction.KeyCode.ARROW_UP),
                // new DirButton("⬀", +1, +1),
                new DirButton("◀", -1,  0).setClickShortcut(ShortcutAction.KeyCode.ARROW_LEFT),
                new DirButton("◆",  0,  0).setClickShortcut(ShortcutAction.KeyCode.ENTER),
                new DirButton("▶", +1,  0).setClickShortcut(ShortcutAction.KeyCode.ARROW_RIGHT),
                // new DirButton("⬃", -1, -1),
                new DirButton("▼",  0, -1).setClickShortcut(ShortcutAction.KeyCode.ARROW_DOWN)
                // new DirButton("⬂", +1, -1)
        };
        for (DirButton button : buttons)
            grid.addComponent(button, button.tx, button.ty);
        return grid;
    }


    protected AbstractComponent makeStepLenSetter() {
        stepLenSetter = new NumSetter("Step length", 1, 10, 100, 1000);
        stepLenSetter.addListener(new Listener() {
            @Override
            public void componentEvent(Event event) {
                stepLen = stepLenSetter.ask();
            }
        });
        stepLen = 1;
        return stepLenSetter;
    }

    LaserJoystick(Laser laser_, Draw draw_) {
        laser = laser_;
        draw = draw_;
        setSpacing(true);
        addComponent(makeDirButtons());
        addComponent(makeStepLenSetter());
        addComponent(coordLabel = new Label("NA"));

    }
}
