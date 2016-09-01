package com.pechatnov;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.*;
import com.vaadin.shared.ui.slider.SliderOrientation;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;


@Theme("mytheme")
@Widgetset("com.pechatnov.MyAppWidgetset")
@Title("Laser manage")
public class MyUI extends UI {
    protected static Laser laser;
    protected static Draw draw;

    static {
        try {
            laser = new Laser();
            draw = new Draw(laser);
        } catch (Exception e) {
            System.out.println("Oh no! Can't init laser and drawer");
            UsefulMethods.notifyException(e);
            System.exit(-7);
        }
    }

    protected Slider scaleSlider;


    protected AbstractLayout makeScaleSetter() {
        scaleSlider = new Slider(1, 15);
        scaleSlider.setOrientation(SliderOrientation.HORIZONTAL);
        scaleSlider.setValue(draw.getScale().doubleValue());
        final Label scaleValue = new Label(draw.getScale().toString()), label = new Label("Scale");
        scaleValue.setSizeUndefined();
        label.setSizeUndefined();
        scaleSlider.addValueChangeListener(
                new Property.ValueChangeListener() {
                    public void valueChange(Property.ValueChangeEvent event) {
                        draw.setScale(scaleSlider.getValue().intValue());
                        scaleValue.setValue(draw.getScale().toString());
                    }
                });
        scaleSlider.setImmediate(true);
        scaleSlider.setWidth("300px");
        VerticalLayout vlayout = new VerticalLayout();
        vlayout.setDefaultComponentAlignment(Alignment.BOTTOM_LEFT);
        vlayout.addComponent(label);
        vlayout.addComponent(UsefulMethods.horisontalLayoutPair(scaleSlider, scaleValue));
        return vlayout;
    }


    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);
        initAll(layout);
    }

    private TreeTable treetable;

    private void initFileTree(ComponentContainer parentLayout) {
        treetable = new TreeTable("File System");
        treetable.setSelectable(true);
        treetable.setColumnCollapsingAllowed(true);
        treetable.setColumnReorderingAllowed(true);
        treetable.setSizeFull();
        treetable.setWidth("300px");
        treetable.setHeight("800px");
        parentLayout.addComponent(treetable);
    }
    private void updateFileTree(File sourcePath) {
        FilesystemContainer currentFileSystem = new FilesystemContainer(sourcePath);
        currentFileSystem.setRecursive(false);
        treetable.setContainerDataSource(currentFileSystem);
        treetable.setItemIconPropertyId("Icon");
        treetable.setVisibleColumns(new String[]{"Name"/*, "Size", "Last Modified"*/});
        treetable.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent itemClickEvent) {
                String clickedFilename = itemClickEvent.getItemId().toString();
                System.out.println("ItemClick: pathname:" + clickedFilename);
                if (itemClickEvent.isDoubleClick()) {
                    doChangeDir(clickedFilename);
                } else {
                    doSelectFile(clickedFilename);
                }
            }
        });
    }
    private File currentPath;

    private void getDefaultDirectory() {
        currentPath = new File("/home/root/pic");
    }

    private String selectedFilename;

    private void doRefresh() {
        updateAll();
    }

    private void doChangeDir(String path) {
        File newCurrentPath = new File(path);
        if (newCurrentPath.isDirectory()) {
            currentPath = newCurrentPath;
            selectedFilename = null;
            updateAll();
        }
    }

    // Пользовательское действие — переход в каталог на уровень выше
    private void doUpLevel() {
        currentPath = currentPath.getParentFile();
        selectedFilename = null;
        updateAll();
    }

    // Пользовательское действие — выбор файла
    private void doSelectFile(String filename) {
        selectedFilename = filename;
        updateInfo();
    }

    private void doDelete() {
        if (selectedFilename == null)
            return;
        try {
            Files.delete(new File(selectedFilename).toPath());
            selectedFilename = null;
            updateFileTree(currentPath);
            updatePreview(selectedFilename);
        } catch (Exception e) {
            UsefulMethods.notifyException(e);
        }
    }

    // Инициализация всех элементов

    private void initAll(VerticalLayout layout) {
        layout.addComponent(new LaserJoystick(laser, draw));
        layout.addComponent(new LaserSettings(laser, draw));
        layout.addComponent(makeScaleSetter());
        initTopPanel(layout);
        initMainPanels(layout);
        getDefaultDirectory();
        updateFileTree(currentPath);
    }


    // Обновление всех элементов
    private void updateAll() {
        updatePreview(selectedFilename);
        updateFileTree(currentPath);
        updateInfo();
    }

    // Обновление информации о файле/каталоге (при изменении файла/каталога)
    private void updateInfo() {
        updateTopPanel(currentPath, selectedFilename);
        updatePreview(selectedFilename);
    }

    private Label labelFileName;

    private void initTopPanel(Layout parentLayout) {
        VerticalLayout verticalLayout = new VerticalLayout();
        HorizontalLayout topPanelLayout = new HorizontalLayout();
        topPanelLayout.setWidth("600px");
        topPanelLayout.setSpacing(true);
        topPanelLayout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

        Button button = new Button("Refresh");
        button.setIcon(FontAwesome.REFRESH);
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                doRefresh();
            }
        });
        topPanelLayout.addComponent(button);

        button = new Button("Up Level");
        button.setIcon(FontAwesome.ARROW_UP);
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                doUpLevel();
            }
        });
        topPanelLayout.addComponent(button);

        button = new Button("Delete");
        button.setIcon(FontAwesome.ERASER);
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                doDelete();
            }
        });
        topPanelLayout.addComponent(button);


        class ImageUploader implements Upload.Receiver, Upload.SucceededListener {
            public File file;

            public OutputStream receiveUpload(String filename,
                                              String mimeType) {
                FileOutputStream fos = null;
                try {
                    file = new File(currentPath.toString() + "/" /*"/home/root/pic/"*/ + filename);
                    fos = new FileOutputStream(file);
                } catch (Exception e) {
                    UsefulMethods.notifyException(e);
                    return null;
                }
                return fos;
            }

            public void uploadSucceeded(Upload.SucceededEvent event) {
                selectedFilename = file.toString();
                updateFileTree(currentPath);
                updatePreview(selectedFilename);
            }
        };
        ImageUploader receiver = new ImageUploader();
        Upload upload = new Upload(null, receiver);
        upload.setButtonCaption("Upload file");
        upload.setImmediate(true);
        upload.addSucceededListener(receiver);
        topPanelLayout.addComponent(upload);

        labelFileName = new Label();
        verticalLayout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

        verticalLayout.addComponent(topPanelLayout);
        verticalLayout.addComponent(labelFileName);

        verticalLayout.setExpandRatio(labelFileName, 1);

        parentLayout.addComponent(verticalLayout);
    }

    private void updateTopPanel(File currentPath, String selectedFilename) {
        if (selectedFilename != null) {
            labelFileName.setValue(selectedFilename);
        } else {
            labelFileName.setValue(currentPath.toString());
        }
    }




    private VerticalLayout previewLayout;
    private Embedded previewImage;
    private Label sizeLabel;

    private void initMainPanels(VerticalLayout parentLayout) {
        HorizontalLayout mainPanels = new HorizontalLayout();
        mainPanels.setSpacing(true);
        parentLayout.addComponent(mainPanels);
        parentLayout.setExpandRatio(mainPanels, 1);
        initFileTree(mainPanels);
        initPreview(mainPanels);
    }

    private void initPreview(ComponentContainer parentLayout) {
        previewLayout = new VerticalLayout();
        sizeLabel = new Label("");
        sizeLabel.setSizeUndefined();
        previewLayout.addComponent(sizeLabel);
        previewImage = new Embedded(null, null);
        previewImage.setVisible(true);
        previewLayout.addComponent(previewImage);

        parentLayout.addComponent(previewLayout);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing(true);
        /* Grayscale and transform flags */ {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setSpacing(true);
            CheckBox grayscaleCheckbox = new CheckBox("enable grayscale"),
                    transformCheckbox = new CheckBox("enable transform");
            grayscaleCheckbox.setValue(laser.isUseGreyscale());
            transformCheckbox.setValue(draw.isUseTransform());
            grayscaleCheckbox.addValueChangeListener(event -> {
                laser.setUseGreyscale(grayscaleCheckbox.getValue());
                updatePreview(selectedFilename);
            });
            transformCheckbox.addValueChangeListener(event -> {
                draw.setUseTransform(transformCheckbox.getValue());
            });
            horizontalLayout.addComponent(grayscaleCheckbox);
            horizontalLayout.addComponent(transformCheckbox);
            verticalLayout.addComponent(horizontalLayout);
        }
        /* Add usual draw methods */ {
            verticalLayout.addComponent(new Label("Usual draw methods"));
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setSpacing(true);
            Button downwardButton = new Button("downward"), upwardButton = new Button("upward"),
                    magicButton = new Button("magic"), showCornersButton = new Button("Show corners");
            horizontalLayout.addComponent(showCornersButton);
            horizontalLayout.addComponent(new Label(" Draw: "));
            horizontalLayout.addComponent(upwardButton);
            horizontalLayout.addComponent(downwardButton);
            horizontalLayout.addComponent(magicButton);
            verticalLayout.addComponent(horizontalLayout);
            downwardButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent clickEvent) {
                    try {
                        if (selectedFilename != null)
                            draw.drawPic(selectedFilename);
                    } catch (Exception e) {
                        UsefulMethods.notifyException(e);
                    }
                }
            });
            upwardButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent clickEvent) {
                    try {
                        if (selectedFilename != null)
                            draw.drawPicUpward(selectedFilename);
                    } catch (Exception e) {
                        UsefulMethods.notifyException(e);
                    }
                }
            });
            magicButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent clickEvent) {
                    try {
                        if (selectedFilename != null)
                            draw.drawPicMagic1(selectedFilename);
                    } catch (Exception e) {
                        UsefulMethods.notifyException(e);
                    }
                }
            });
            showCornersButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent clickEvent) {
                    try {
                        if (selectedFilename != null)
                            draw.showCorners(selectedFilename);
                    } catch (Exception e) {
                        UsefulMethods.notifyException(e);
                    }
                }
            });
        }
        parentLayout.addComponent(verticalLayout);
    }

    private void clearPreview() {
        previewImage.setSource(null);
        previewImage.setVisible(true);
        sizeLabel.setValue("Not a .bmp picture");
    }


    private void updatePreview(String pathname) {
        if (pathname == null || pathname.length() == 0) {
            clearPreview();
            return;
        }
        File file = new File(pathname);
        int lastIndexOf = pathname.lastIndexOf(".");
        String extension = (lastIndexOf == -1) ? "" : pathname.substring(lastIndexOf);

        final int PREVIEW_FILE_LIMIT = 1024 * 1024;

        previewImage.setVisible(false);
        if (file.length() > PREVIEW_FILE_LIMIT) {
            clearPreview();
            return;
        }
        if (".bmp".equals(extension)) {
            MImage image = MImage.openImage(file.toString());
            if (image == null)
                return;
            sizeLabel.setValue("Size: " + image.W.toString() + "x" + image.H.toString());
            if (laser.isUseGreyscale())
                previewImage.setSource(image.getGraySource());
            else
                previewImage.setSource(image.getBlackAndWhiteSource());
            previewImage.setVisible(true);
            previewLayout.setExpandRatio(previewImage, 1.0f);
        }
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}

