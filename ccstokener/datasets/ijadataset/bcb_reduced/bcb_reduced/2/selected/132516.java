package org.xith3d.ui.hud.utils;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.vecmath.Color3f;
import javax.vecmath.Point2f;
import org.xith3d.ui.hud.HUD;
import org.xith3d.ui.hud.base.LabeledStateButton;
import org.xith3d.ui.hud.base.StateButton;
import org.xith3d.ui.hud.base.Border;
import org.xith3d.ui.hud.base.WindowHeaderWidget;
import org.xith3d.ui.hud.widgets.Button;
import org.xith3d.ui.hud.widgets.ComboBox;
import org.xith3d.ui.hud.widgets.Label;
import org.xith3d.ui.hud.widgets.List;
import org.xith3d.ui.hud.widgets.Panel;
import org.xith3d.ui.hud.widgets.ProgressBar;
import org.xith3d.ui.hud.widgets.Scrollbar;
import org.xith3d.ui.hud.widgets.Slider;
import org.xith3d.ui.hud.widgets.TextField;
import org.xith3d.ui.text2d.TextAlignment;
import org.xith3d.loaders.texture.TextureLoader;
import org.xith3d.loaders.texture.TextureStreamLocatorZip;
import org.xith3d.scenegraph.Texture;

/**
 * Extend this class when you create a Widget-theme for your HUD.
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class WidgetTheme {

    protected ThemeProperties themeProps;

    private Label.Description labelDesc = null;

    private TextField.Description textFieldDesc = null;

    private Button.Description buttonDescNormal = null;

    private Button.Description buttonDescHovered = null;

    private Button.Description buttonDescPressed = null;

    private Border.Description roundedCornersBorderDesc = null;

    private Border.Description loweredBevelBorderDesc = null;

    private Border.Description raisedBevelBorderDesc = null;

    private Scrollbar.Description scrollbarDescHoriz = null;

    private Scrollbar.Description scrollbarDescVert = null;

    private Slider.Description sliderDesc = null;

    private LabeledStateButton.Description radioButtonDesc = null;

    private LabeledStateButton.Description checkboxDesc = null;

    private WindowHeaderWidget.Description windowHeaderDesc = null;

    private Border.Description windowBorderDesc = null;

    private List.Description listDesc = null;

    private ComboBox.Description comboDesc = null;

    private ProgressBar.Description progressbarDesc = null;

    private Panel.Description panelDesc = null;

    /**
     * @return the name of this WidgetTheme
     */
    public String getName() {
        return (themeProps.name);
    }

    /**
     * Sets the default Font of this theme.
     */
    public void setFont(Font font) {
        themeProps.font = font;
    }

    /**
     * @return the default Font of this theme
     */
    public Font getFont() {
        return (themeProps.font);
    }

    /**
     * Sets the default font-color of this theme.
     */
    public void setFontColor(Color3f color) {
        themeProps.font_color = color;
    }

    /**
     * @return the default font-color of this theme
     */
    public Color3f getFontColor() {
        return (themeProps.font_color);
    }

    /**
     * Sets the default Label.Description.
     */
    public void setLabelDescription(Label.Description desc) {
        this.labelDesc = desc;
    }

    /**
     * @return the default Label.Description
     */
    public Label.Description getLabelDescription() {
        if (this.labelDesc == null) {
            Label.Description labelDesc = new Label.Description();
            labelDesc.setFont(themeProps.font);
            labelDesc.setFontColor(themeProps.font_color);
            this.labelDesc = labelDesc;
        }
        return (this.labelDesc.clone());
    }

    /**
     * Sets the default TextField.Description.
     */
    public void setTextFieldDescription(TextField.Description desc) {
        this.textFieldDesc = desc;
    }

    /**
     * @return the default TextField.Description
     */
    public TextField.Description getTextFieldDescription() {
        if (this.textFieldDesc == null) {
            TextField.Description textFieldDesc = new TextField.Description();
            Label.Description labelDesc = getLabelDescription();
            labelDesc.setBackground(themeProps.textfield_background_color);
            textFieldDesc.setLabelDescription(labelDesc);
            Border.Description borderDesc = new Border.Description(themeProps.textfield_border_size_bottom, themeProps.textfield_border_size_right, themeProps.textfield_border_size_top, themeProps.textfield_border_size_left);
            borderDesc.collectTexturesByBaseName(getName() + "/borders/textfield/normal/", "png", themeProps.textfield_border_textures_alpha);
            textFieldDesc.setBorderDescription(borderDesc);
            this.textFieldDesc = textFieldDesc;
        }
        return (this.textFieldDesc.clone());
    }

    public int getTextCaretWidth() {
        return (themeProps.caret_text_width);
    }

    public Texture getTextCaretTexture() {
        return (TextureLoader.getInstance().getTexture(getName() + "/carets/text-caret.png", (themeProps.caret_text_texture_alpha ? Texture.RGBA : Texture.RGB)));
    }

    /**
     * Sets the default Scrollbar.Description for a HORIZONTAL Scrollbar.
     */
    public void setScrollbarDescriptionHorizontal(Scrollbar.Description desc) {
        this.scrollbarDescHoriz = desc;
    }

    /**
     * @return the default Scrollbar.Description for a HORIZONTAL Scrollbar
     */
    public Scrollbar.Description getScrollbarDescriptionHorizontal() {
        if (this.scrollbarDescHoriz == null) {
            Scrollbar.Description sbDesc = new Scrollbar.Description(Scrollbar.Direction.HORIZONTAL);
            sbDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            sbDesc.setSize(themeProps.scrollbar_size);
            sbDesc.setBackgroundTexture(getName() + "/scrollbar/horizontal/background.png", false);
            sbDesc.setDecrementTexture(getName() + "/scrollbar/horizontal/decrementor.png", true);
            sbDesc.setIncrementTexture(getName() + "/scrollbar/horizontal/incrementor.png", true);
            sbDesc.setHandleTexture(getName() + "/scrollbar/horizontal/handle.png", true);
            sbDesc.setDecrementButtonSize(themeProps.scrollbar_decrementor_size);
            sbDesc.setIncrementButtonSize(themeProps.scrollbar_incrementor_size);
            sbDesc.setHandleButtonSize(themeProps.scrollbar_handle_size);
            sbDesc.setLower(themeProps.scrollbar_values_lower);
            sbDesc.setUpper(themeProps.scrollbar_values_upper);
            sbDesc.setSmallIncrement(themeProps.scrollbar_values_smallincrement);
            sbDesc.setSmoothScrolling(themeProps.scrollbar_smoothscrolling);
            this.scrollbarDescHoriz = sbDesc;
        }
        return (this.scrollbarDescHoriz.clone());
    }

    /**
     * Sets the default Scrollbar.Description for a VERTICAL Scrollbar.
     */
    public void setScrollbarDescriptionVertical(Scrollbar.Description desc) {
        this.scrollbarDescVert = desc;
    }

    /**
     * @return the default Scrollbar.Description for a VERTICAL Scrollbar
     */
    public Scrollbar.Description getScrollbarDescriptionVertical() {
        if (this.scrollbarDescVert == null) {
            Scrollbar.Description sbDesc = new Scrollbar.Description(Scrollbar.Direction.VERTICAL);
            sbDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            sbDesc.setSize(themeProps.scrollbar_size);
            sbDesc.setBackgroundTexture(getName() + "/scrollbar/vertical/background.png", false);
            sbDesc.setDecrementTexture(getName() + "/scrollbar/vertical/decrementor.png", true);
            sbDesc.setIncrementTexture(getName() + "/scrollbar/vertical/incrementor.png", true);
            sbDesc.setHandleTexture(getName() + "/scrollbar/vertical/handle.png", true);
            sbDesc.setDecrementButtonSize(themeProps.scrollbar_decrementor_size);
            sbDesc.setIncrementButtonSize(themeProps.scrollbar_incrementor_size);
            sbDesc.setHandleButtonSize(themeProps.scrollbar_handle_size);
            sbDesc.setLower(themeProps.scrollbar_values_lower);
            sbDesc.setUpper(themeProps.scrollbar_values_upper);
            sbDesc.setSmallIncrement(themeProps.scrollbar_values_smallincrement);
            sbDesc.setSmoothScrolling(themeProps.scrollbar_smoothscrolling);
            this.scrollbarDescVert = sbDesc;
        }
        return (this.scrollbarDescVert.clone());
    }

    /**
     * @return the texture to use for the space in the lower-right corner of a ScrollPane
     */
    public Texture getScrollPanelSpaceTexture() {
        return (TextureLoader.getInstance().getTexture(getName() + "/scrollpane/spacer.png", Texture.RGB));
    }

    /**
     * Sets the default Slider.Description.
     */
    public void setSliderDescription(Slider.Description desc) {
        this.sliderDesc = desc;
    }

    /**
     * @return the default Slider.Description
     */
    public Slider.Description getSliderDescription() {
        if (this.sliderDesc == null) {
            Slider.Description desc = new Slider.Description();
            desc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            desc.setLeftTexture(getName() + "/slider/left.png", true);
            desc.setRightTexture(getName() + "/slider/right.png", true);
            desc.setBodyTexture(getName() + "/slider/body.png", true);
            desc.setHandleTexture(getName() + "/slider/handle.png", true);
            desc.setHeight(themeProps.slider_size_height);
            desc.setLeftWidth(themeProps.slider_left_width);
            desc.setRightWidth(themeProps.slider_right_width);
            desc.setHandleButtonYOffset(themeProps.slider_handle_yoffset);
            desc.setHandleButtonWidth(themeProps.slider_handle_width);
            desc.setLower(themeProps.slider_values_lower);
            desc.setUpper(themeProps.slider_values_upper);
            desc.setSmoothSliding(themeProps.slider_smoothsliding);
            this.sliderDesc = desc;
        }
        return (this.sliderDesc.clone());
    }

    /**
     * Sets the default Button.Description for NORMAL state.
     */
    public void setButtonDescriptionNormal(Button.Description desc) {
        this.buttonDescNormal = desc;
    }

    /**
     * @return the default Button.Description for NORMAL state
     */
    public Button.Description getButtonDescriptionNormal() {
        if (this.buttonDescNormal == null) {
            Button.Description buttonDesc = new Button.Description(themeProps.button_size_bottom, themeProps.button_size_right, themeProps.button_size_top, themeProps.button_size_left);
            buttonDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            buttonDesc.setBodyTexture(getName() + "/button/normal/body.png", themeProps.button_textures_alpha);
            buttonDesc.setBottomTexture(getName() + "/button/normal/bottom.png", themeProps.button_textures_alpha);
            buttonDesc.setRightTexture(getName() + "/button/normal/right.png", themeProps.button_textures_alpha);
            buttonDesc.setTopTexture(getName() + "/button/normal/top.png", themeProps.button_textures_alpha);
            buttonDesc.setLeftTexture(getName() + "/button/normal/left.png", themeProps.button_textures_alpha);
            buttonDesc.setLLTexture(getName() + "/button/normal/ll.png", themeProps.button_textures_alpha);
            buttonDesc.setLRTexture(getName() + "/button/normal/lr.png", themeProps.button_textures_alpha);
            buttonDesc.setURTexture(getName() + "/button/normal/ur.png", themeProps.button_textures_alpha);
            buttonDesc.setULTexture(getName() + "/button/normal/ul.png", themeProps.button_textures_alpha);
            this.buttonDescNormal = buttonDesc;
        }
        return (this.buttonDescNormal.clone());
    }

    /**
     * Sets the default Button.Description for HOVERED state.
     */
    public void setButtonDescriptionHovered(Button.Description desc) {
        this.buttonDescHovered = desc;
    }

    /**
     * @return the default Button.Description for HOVERED state
     */
    public Button.Description getButtonDescriptionHovered() {
        if (this.buttonDescHovered == null) {
            Button.Description buttonDesc = new Button.Description(themeProps.button_size_bottom, themeProps.button_size_right, themeProps.button_size_top, themeProps.button_size_left);
            buttonDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            buttonDesc.setBodyTexture(getName() + "/button/hovered/body.png", themeProps.button_textures_alpha);
            buttonDesc.setBottomTexture(getName() + "/button/hovered/bottom.png", themeProps.button_textures_alpha);
            buttonDesc.setRightTexture(getName() + "/button/hovered/right.png", themeProps.button_textures_alpha);
            buttonDesc.setTopTexture(getName() + "/button/hovered/top.png", themeProps.button_textures_alpha);
            buttonDesc.setLeftTexture(getName() + "/button/hovered/left.png", themeProps.button_textures_alpha);
            buttonDesc.setLLTexture(getName() + "/button/hovered/ll.png", themeProps.button_textures_alpha);
            buttonDesc.setLRTexture(getName() + "/button/hovered/lr.png", themeProps.button_textures_alpha);
            buttonDesc.setURTexture(getName() + "/button/hovered/ur.png", themeProps.button_textures_alpha);
            buttonDesc.setULTexture(getName() + "/button/hovered/ul.png", themeProps.button_textures_alpha);
            this.buttonDescHovered = buttonDesc;
        }
        return (this.buttonDescHovered.clone());
    }

    /**
     * Sets the default Button.Description for PRESSED state.
     */
    public void setButtonDescriptionPressed(Button.Description desc) {
        this.buttonDescPressed = desc;
    }

    /**
     * @return the default Button.Description for PRESSED state
     */
    public Button.Description getButtonDescriptionPressed() {
        if (this.buttonDescPressed == null) {
            Button.Description buttonDesc = new Button.Description(themeProps.button_size_bottom, themeProps.button_size_right, themeProps.button_size_top, themeProps.button_size_left);
            buttonDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            buttonDesc.setBodyTexture(getName() + "/button/pressed/body.png", themeProps.button_textures_alpha);
            buttonDesc.setBottomTexture(getName() + "/button/pressed/bottom.png", themeProps.button_textures_alpha);
            buttonDesc.setRightTexture(getName() + "/button/pressed/right.png", themeProps.button_textures_alpha);
            buttonDesc.setTopTexture(getName() + "/button/pressed/top.png", themeProps.button_textures_alpha);
            buttonDesc.setLeftTexture(getName() + "/button/pressed/left.png", themeProps.button_textures_alpha);
            buttonDesc.setLLTexture(getName() + "/button/pressed/ll.png", themeProps.button_textures_alpha);
            buttonDesc.setLRTexture(getName() + "/button/pressed/lr.png", themeProps.button_textures_alpha);
            buttonDesc.setURTexture(getName() + "/button/pressed/ur.png", themeProps.button_textures_alpha);
            buttonDesc.setULTexture(getName() + "/button/pressed/ul.png", themeProps.button_textures_alpha);
            this.buttonDescPressed = buttonDesc;
        }
        return (this.buttonDescPressed.clone());
    }

    /**
     * Sets the default RadioButton.Description.
     */
    public void setRadioButtonDescription(LabeledStateButton.Description desc) {
        this.radioButtonDesc = desc;
    }

    /**
     * @return the default RadioButton.Description
     */
    public LabeledStateButton.Description getRadioButtonDescription() {
        if (this.radioButtonDesc == null) {
            LabeledStateButton.Description radioButtonDesc = new LabeledStateButton.Description();
            radioButtonDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            radioButtonDesc.setTexture(StateButton.State.DEACTIVATED_NORMAL, getName() + "/radiobutton/deactivated-normal.png", themeProps.radiobutton_textures_alpha);
            radioButtonDesc.setTexture(StateButton.State.DEACTIVATED_HOVERED, getName() + "/radiobutton/deactivated-hovered.png", themeProps.radiobutton_textures_alpha);
            radioButtonDesc.setTexture(StateButton.State.ACTIVATED_NORMAL, getName() + "/radiobutton/activated-normal.png", themeProps.radiobutton_textures_alpha);
            radioButtonDesc.setTexture(StateButton.State.ACTIVATED_HOVERED, getName() + "/radiobutton/activated-hovered.png", themeProps.radiobutton_textures_alpha);
            radioButtonDesc.setImageSize(new Point2f(themeProps.radiobutton_image_size, themeProps.radiobutton_image_size));
            radioButtonDesc.setSpace(themeProps.radiobutton_space_size);
            radioButtonDesc.setLabelDescription(getLabelDescription());
            radioButtonDesc.getLabelDescription().setAlignment(TextAlignment.CENTER_LEFT);
            this.radioButtonDesc = radioButtonDesc;
        }
        return (this.radioButtonDesc.clone());
    }

    /**
     * Sets the default CheckBox.Description.
     */
    public void setCheckBoxDescription(LabeledStateButton.Description desc) {
        this.checkboxDesc = desc;
    }

    /**
     * @return the default CheckBox.Description
     */
    public LabeledStateButton.Description getCheckBoxDescription() {
        if (this.checkboxDesc == null) {
            LabeledStateButton.Description checkboxDesc = new LabeledStateButton.Description();
            checkboxDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            checkboxDesc.setTexture(StateButton.State.DEACTIVATED_NORMAL, getName() + "/checkbox/unchecked-normal.png", themeProps.checkbox_textures_alpha);
            checkboxDesc.setTexture(StateButton.State.DEACTIVATED_HOVERED, getName() + "/checkbox/unchecked-hovered.png", themeProps.checkbox_textures_alpha);
            checkboxDesc.setTexture(StateButton.State.ACTIVATED_NORMAL, getName() + "/checkbox/checked-normal.png", themeProps.checkbox_textures_alpha);
            checkboxDesc.setTexture(StateButton.State.ACTIVATED_HOVERED, getName() + "/checkbox/checked-hovered.png", themeProps.checkbox_textures_alpha);
            checkboxDesc.setImageSize(new Point2f(themeProps.checkbox_image_size, themeProps.checkbox_image_size));
            checkboxDesc.setSpace(themeProps.checkbox_space_size);
            checkboxDesc.setLabelDescription(getLabelDescription());
            checkboxDesc.getLabelDescription().setAlignment(TextAlignment.CENTER_LEFT);
            this.checkboxDesc = checkboxDesc;
        }
        return (this.checkboxDesc.clone());
    }

    /**
     * Sets the RoundedCorners Border.Description.
     */
    public void setRoundedCornersBorderDescription(Border.Description desc) {
        this.roundedCornersBorderDesc = desc;
    }

    /**
     * @return the RoundedCorners Border.Description
     */
    public Border.Description getRoundedCornersBorderDescription() {
        if (this.roundedCornersBorderDesc == null) {
            Border.Description borderDesc = new Border.Description(themeProps.border_rounded_corners_size_bottom, themeProps.border_rounded_corners_size_right, themeProps.border_rounded_corners_size_top, themeProps.border_rounded_corners_size_left);
            borderDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            borderDesc.collectTexturesByBaseName(getName() + "/borders/rounded_corners/", "png", themeProps.border_rounded_corners_textures_alpha, themeProps.border_rounded_corners_size_ll_upper, themeProps.border_rounded_corners_size_ll_right, themeProps.border_rounded_corners_size_lr_left, themeProps.border_rounded_corners_size_lr_upper, themeProps.border_rounded_corners_size_ur_lower, themeProps.border_rounded_corners_size_ur_left, themeProps.border_rounded_corners_size_ul_right, themeProps.border_rounded_corners_size_ul_lower);
            this.roundedCornersBorderDesc = borderDesc;
        }
        return (this.roundedCornersBorderDesc.clone());
    }

    /**
     * @return the standard Border.Description
     * By default this is "rounded courners".
     */
    public Border.Description getStandardBorderDescription() {
        return (getRoundedCornersBorderDescription());
    }

    /**
     * Sets the default Border.Description.
     */
    public void setLoweredBevelBorderDescription(Border.Description desc) {
        this.loweredBevelBorderDesc = desc;
    }

    /**
     * @return the default Border.Description
     */
    public Border.Description getLoweredBevelBorderDescription() {
        if (this.loweredBevelBorderDesc == null) {
            Border.Description borderDesc = new Border.Description(themeProps.border_bevel_lowered_size_bottom, themeProps.border_bevel_lowered_size_right, themeProps.border_bevel_lowered_size_top, themeProps.border_bevel_lowered_size_left);
            borderDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            borderDesc.collectTexturesByBaseName(getName() + "/borders/bevel/lowered/", "png", themeProps.border_bevel_lowered_textures_alpha, themeProps.border_bevel_lowered_size_ll_upper, themeProps.border_bevel_lowered_size_ll_right, themeProps.border_bevel_lowered_size_lr_left, themeProps.border_bevel_lowered_size_lr_upper, themeProps.border_bevel_lowered_size_ur_lower, themeProps.border_bevel_lowered_size_ur_left, themeProps.border_bevel_lowered_size_ul_right, themeProps.border_bevel_lowered_size_ul_lower);
            this.loweredBevelBorderDesc = borderDesc;
        }
        return (this.loweredBevelBorderDesc.clone());
    }

    /**
     * Sets the default Border.Description.
     */
    public void setRaisedBevelBorderDescription(Border.Description desc) {
        this.raisedBevelBorderDesc = desc;
    }

    /**
     * @return the default Border.Description
     */
    public Border.Description getRaisedBevelBorderDescription() {
        if (this.raisedBevelBorderDesc == null) {
            Border.Description borderDesc = new Border.Description(themeProps.border_bevel_raised_size_bottom, themeProps.border_bevel_raised_size_right, themeProps.border_bevel_raised_size_top, themeProps.border_bevel_raised_size_left);
            borderDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            borderDesc.collectTexturesByBaseName(getName() + "/borders/bevel/raised/", "png", themeProps.border_bevel_raised_textures_alpha, themeProps.border_bevel_raised_size_ll_upper, themeProps.border_bevel_raised_size_ll_right, themeProps.border_bevel_raised_size_lr_left, themeProps.border_bevel_raised_size_lr_upper, themeProps.border_bevel_raised_size_ur_lower, themeProps.border_bevel_raised_size_ur_left, themeProps.border_bevel_raised_size_ul_right, themeProps.border_bevel_raised_size_ul_lower);
            this.raisedBevelBorderDesc = borderDesc;
        }
        return (this.raisedBevelBorderDesc.clone());
    }

    private Border.Description getBorderDescriptionByName(String name) {
        if (name == null) return (null); else if (name.equals("rounded_corners")) return (getRoundedCornersBorderDescription()); else if (name.equals("bevel/lowered")) return (getLoweredBevelBorderDescription()); else if (name.equals("bevel/raised")) return (getRaisedBevelBorderDescription()); else if (name.equals("frame")) return (getFrameBorderDescription());
        throw (new IllegalArgumentException("Unknown named Border.Description \"" + name + "\""));
    }

    /**
     * Sets the default WindowHeaderWidget.Description.
     */
    public void setWindowHeaderDescription(WindowHeaderWidget.Description desc) {
        this.windowHeaderDesc = desc;
    }

    /**
     * @return the default WindowHeaderWidget.Description
     */
    public WindowHeaderWidget.Description getWindowHeaderDescription() {
        if (this.windowHeaderDesc == null) {
            WindowHeaderWidget.Description headerDesc = new WindowHeaderWidget.Description(HUDUnitsMeasurement.PIXELS);
            headerDesc.setHeight(themeProps.frame_title_height);
            headerDesc.setCloseButtonSize(new Point2f(themeProps.frame_title_closebutton_width, themeProps.frame_title_closebutton_height));
            headerDesc.setLabelDescription(getLabelDescription());
            headerDesc.getLabelDescription().setFont(themeProps.frame_title_font);
            headerDesc.getLabelDescription().setFontColor(themeProps.frame_title_font_color);
            headerDesc.getLabelDescription().setAlignment(TextAlignment.CENTER_CENTER);
            headerDesc.getLabelDescription().setBackground(getName() + "/frame/title-background.png", false);
            headerDesc.setCloseButtonDescriptionNormal(new Button.Description(themeProps.frame_title_textures_alpha, getName() + "/frame/close-normal.png"));
            headerDesc.setCloseButtonDescriptionHovered(new Button.Description(themeProps.frame_title_textures_alpha, getName() + "/frame/close-hovered.png"));
            headerDesc.setCloseButtonDescriptionPressed(new Button.Description(themeProps.frame_title_textures_alpha, getName() + "/frame/close-pressed.png"));
            this.windowHeaderDesc = headerDesc;
        }
        return (this.windowHeaderDesc.clone());
    }

    /**
     * Sets the default Border.Description for a Frame.
     */
    public void setFrameBorderDescription(Border.Description desc) {
        this.windowBorderDesc = desc;
    }

    /**
     * @return the default Border.Description for a Frame
     */
    public Border.Description getFrameBorderDescription() {
        if (this.windowBorderDesc == null) {
            Border.Description borderDesc = new Border.Description(themeProps.border_frame_size_bottom, themeProps.border_frame_size_right, themeProps.border_frame_size_top, themeProps.border_frame_size_left);
            borderDesc.collectTexturesByBaseName(getName() + "/borders/frame/", "png", themeProps.border_frame_textures_alpha, themeProps.border_frame_size_ll_upper, themeProps.border_frame_size_ll_right, themeProps.border_frame_size_lr_left, themeProps.border_frame_size_lr_upper, themeProps.border_frame_size_ur_lower, themeProps.border_frame_size_ur_left, themeProps.border_frame_size_ul_right, themeProps.border_frame_size_ul_lower);
            this.windowBorderDesc = borderDesc;
        }
        return (this.windowBorderDesc.clone());
    }

    /**
     * Sets the default Font of this theme.
     */
    public void setProgressBarLabelFont(Font font) {
        themeProps.progressbar_label_font = font;
    }

    /**
     * @return the default Font of this theme
     */
    public Font getProgressBarLabelFont() {
        return (themeProps.progressbar_label_font);
    }

    /**
     * Sets the default font-color of this theme.
     */
    public void setProgressBarLabelFontColor(Color3f color) {
        themeProps.progressbar_label_font_color = color;
    }

    /**
     * @return the default font-color of this theme
     */
    public Color3f getProgressBarLabelFontColor() {
        return (themeProps.progressbar_label_font_color);
    }

    /**
     * Sets the default List.Description.
     */
    public void setListDescription(List.Description desc) {
        this.listDesc = desc;
    }

    /**
     * @return the default List.Description
     */
    public List.Description getListDescription() {
        if (this.listDesc == null) {
            List.Description listDesc = new List.Description();
            listDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            listDesc.setPadding(themeProps.list_padding_bottom, themeProps.list_padding_right, themeProps.list_padding_top, themeProps.list_padding_left);
            listDesc.setBorderDescription(getBorderDescriptionByName(themeProps.list_border_name));
            if (themeProps.list_background_color != null) {
                if (themeProps.list_background_color.x < 0.0f) listDesc.setBackgroundTexture(getName() + "/list/background.png", themeProps.list_background_texture_alpha); else listDesc.setBackgroundColor(themeProps.list_background_color);
            }
            listDesc.setSelectionBackground(themeProps.list_selection_background);
            listDesc.setSelectionForeground(themeProps.list_selection_foreground);
            this.listDesc = listDesc;
        }
        return (this.listDesc.clone());
    }

    /**
     * Sets the default ComboBox.Description.
     */
    public void setComboBoxDescription(ComboBox.Description desc) {
        this.comboDesc = desc;
    }

    /**
     * @return the default ComboBox.Description
     */
    public ComboBox.Description getComboBoxDescription() {
        if (this.comboDesc == null) {
            ComboBox.Description comboDesc = new ComboBox.Description();
            comboDesc.setTextFieldDescription(getTextFieldDescription());
            List.Description listDesc = getListDescription();
            listDesc.setBorderDescription(getBorderDescriptionByName(themeProps.combobox_list_border_name));
            listDesc.setHoverBackground(themeProps.combobox_list_hover_background);
            listDesc.setHoverForeground(themeProps.combobox_list_hover_foreground);
            comboDesc.setListDescription(listDesc);
            comboDesc.setButtonSymbol(getName() + "/combobox/button_symbol.png", true);
            this.comboDesc = comboDesc;
        }
        return (this.comboDesc.clone());
    }

    /**
     * Sets the default ProgressBar.Description.
     */
    public void setProgressBarDescription(ProgressBar.Description desc) {
        this.progressbarDesc = desc;
    }

    /**
     * @return the default ProgressBar.Description
     */
    public ProgressBar.Description getProgressBarDescription() {
        if (this.progressbarDesc == null) {
            ProgressBar.Description pbDesc = new ProgressBar.Description(themeProps.progressbar_bar_left_width, themeProps.progressbar_bar_right_width, themeProps.progressbar_border_size);
            pbDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            pbDesc.setBarBodyTexture(getName() + "/progressbar/body.png", false);
            if (themeProps.progressbar_bar_left_width > 0) pbDesc.setBarLeftTexture(getName() + "/progressbar/left.png", themeProps.progressbar_textures_alpha);
            if (themeProps.progressbar_bar_right_width > 0) pbDesc.setBarRightTexture(getName() + "/progressbar/right.png", themeProps.progressbar_textures_alpha);
            pbDesc.setBorderDescription(getBorderDescriptionByName(themeProps.progressbar_border_name));
            this.progressbarDesc = pbDesc;
        }
        return (this.progressbarDesc.clone());
    }

    /**
     * Sets the default Panel.Description.
     */
    public void setPanelDescription(Panel.Description desc) {
        this.panelDesc = desc;
    }

    /**
     * @return the default Panel.Description
     */
    public Panel.Description getPanelDescription() {
        if (this.panelDesc == null) {
            Panel.Description panelDesc = new Panel.Description();
            panelDesc.setMeasurement(HUDUnitsMeasurement.PIXELS);
            panelDesc.setPadding(themeProps.panel_padding_bottom, themeProps.panel_padding_right, themeProps.panel_padding_top, themeProps.panel_padding_left);
            panelDesc.setBorderDescription(getBorderDescriptionByName(themeProps.panel_border_name));
            if (themeProps.panel_background_color != null) {
                if (themeProps.panel_background_color.x < 0.0f) panelDesc.setBackgroundTexture(getName() + "/panel/background.png", themeProps.panel_background_texture_alpha); else panelDesc.setBackgroundColor(themeProps.panel_background_color);
            }
            this.panelDesc = panelDesc;
        }
        return (this.panelDesc.clone());
    }

    protected WidgetTheme(ThemeProperties themeProps) {
        this.themeProps = themeProps;
    }

    protected static ThemeProperties loadThemeProps(InputStream in) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry en;
        while ((en = zipIn.getNextEntry()) != null) {
            if (en.getName().equals("theme.properties")) {
                return (new ThemeProperties(zipIn));
            }
        }
        throw (new IOException("No \"theme.properties\" entry found in the theme archive."));
    }

    /**
     * Creates the desired WidgetTheme.
     * 
     * @param url a URL pointing to the theme-zip-archive
     */
    public WidgetTheme(URL url) throws IOException {
        this(loadThemeProps(url.openStream()));
        TextureLoader.getInstance().addTextureStreamLocator(new TextureStreamLocatorZip(url, "textures/"));
    }

    private static URL getThemeResource(String name) throws IOException {
        URL resource = HUD.class.getClassLoader().getResource("resources/xith3d/hud/themes/" + name + ".xwt");
        if (resource == null) {
            throw (new IOException("The Theme resource with the name \"" + name + "\" was not found in the classpath."));
        }
        return (resource);
    }

    /**
     * Creates the desired <b>built-in</b> WidgetTheme.<br>
     * <br>
     * The theme is loaded from classpath as a resource from path
     * "resources/xith3d/hud/themes/[THEME_NAME].xwt".
     * 
     * @param name the name of the WidgetTheme. <b>Default-Theme</b>: <i>"GTK"</i>
     */
    public WidgetTheme(String name) throws IOException {
        this(getThemeResource(name));
    }

    private static URL file2url(File file) throws IOException {
        try {
            return (file.toURI().toURL());
        } catch (MalformedURLException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw (ioe);
        }
    }

    /**
     * Creates the desired WidgetTheme.
     * 
     * @param zipFile A File representation of the zip-archive of the theme
     */
    public WidgetTheme(File zipFile) throws IOException {
        this(file2url(zipFile));
    }
}
