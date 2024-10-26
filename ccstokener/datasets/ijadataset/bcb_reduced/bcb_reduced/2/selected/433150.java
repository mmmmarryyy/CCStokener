package org.reprap.scanning.FileIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import java.text.DecimalFormat;
import javax.swing.DefaultListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import org.reprap.scanning.FunctionLibraries.ImageFile;

public class MainPreferences {

    public class Papersize {

        public String Name;

        public double width;

        public double height;

        public Papersize(String name, double Width, double Height) {
            Name = name;
            width = Width;
            height = Height;
        }

        public boolean equals(Papersize other) {
            return ((width == other.width) && (height == other.height) && (Name.equals(other.Name)));
        }

        public Papersize clone() {
            return new Papersize(Name, width, height);
        }
    }

    private static final String propsFile = "reprapscanning.properties";

    private static final String propsFolder = ".reprap";

    public static String path;

    public boolean SaveOnProgramWindowClose;

    public boolean SaveOnProgramCancel;

    public boolean SaveOnProgramFinish;

    public boolean SaveCalibrationSheetProperties;

    public boolean SaveProcessedImageProperties;

    public boolean SaveRestrictedSearchImageProperties;

    public boolean[] SkipStep;

    public boolean BlankOutputFilenameOnLoad;

    public DefaultListModel imagefiles;

    public DefaultComboBoxModel calibrationpatterns;

    public int CurrentCalibrationPatternIndexNumber;

    public DefaultComboBoxModel PaperSizeList;

    public int CurrentPaperSizeIndexNumber;

    public ButtonGroup PaperOrientation;

    public JRadioButton PaperOrientationIsPortrait;

    public JRadioButton PaperOrientationIsLandscape;

    public JCheckBox PaperSizeIsCustom;

    public JFormattedTextField PaperCustomSizeWidthmm;

    public JFormattedTextField PaperCustomSizeHeightmm;

    public JFormattedTextField PaperMarginHorizontalmm;

    public JFormattedTextField PaperMarginVerticalmm;

    public JCheckBox CalibrationSheetKeepAspectRatioWhenPrinted;

    public JTextField OutputFileName;

    public JTextField OutputObjectName;

    public int AlgorithmSettingEdgeStrengthThreshold;

    public int AlgorithmSettingCalibrationSheetEdgeStrengthThreshold;

    public int AlgorithmSettingResampledImageWidthForEllipseDetection;

    public int AlgorithmSettingEllipseValidityThresholdPercentage;

    public int AlgorithmSettingMinimumFoundValidEllipses;

    public double AlgorithmSettingPointPairMatchingDistanceThreshold;

    public int AlgorithmSettingMaxBundleAdjustmentNumberOfIterations;

    public int AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees;

    public int AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment;

    public int AlgorithmSettingVolumeSubDivision;

    public double AlgorithmSettingTextureMatchingAngleAccuracyInDegrees;

    public int AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples;

    public double AlgorithmSettingTextureMatchingMinimumSimilarityRange;

    public double AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex;

    public double AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees;

    public double AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative;

    public double AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity;

    public double AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit;

    public boolean AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment;

    public String DebugSaveOutputImagesFolder;

    public boolean DebugImageOverlay;

    public boolean DebugShow3DFlythough;

    public int DebugShow3DFlythoughImagePixelStep;

    public boolean DebugImageSegmentation;

    public boolean DebugCalibrationSheetBarycentricEstimate;

    public boolean DebugCalibrationSheetPlanarHomographyEstimate;

    public boolean DebugRestrictedSearch;

    public boolean DebugEdgeFindingForEllipseDetection;

    public boolean DebugEllipseFinding;

    public boolean DebugManualEllipseSelection;

    public boolean DebugPointPairMatching;

    public boolean DebugPointPairMatchingSubsets;

    public boolean DebugVoxelisation;

    public boolean DebugIndividualTextureMatch;

    public boolean DebugMarchingTextureMatch;

    public boolean Debug;

    public MainPreferences(int numberofsteps, String filepath) {
        init(numberofsteps, filepath);
    }

    public MainPreferences(int numberofsteps) {
        String filepath = new String(System.getProperty("user.home") + File.separatorChar + propsFolder + File.separatorChar + propsFile);
        init(numberofsteps, filepath);
    }

    private void init(int numberofsteps, String filepath) {
        path = filepath;
        SaveOnProgramWindowClose = true;
        SaveOnProgramCancel = false;
        SaveOnProgramFinish = true;
        SaveCalibrationSheetProperties = true;
        SaveRestrictedSearchImageProperties = false;
        SaveProcessedImageProperties = false;
        DebugIndividualTextureMatch = false;
        DebugMarchingTextureMatch = false;
        DebugImageOverlay = false;
        DebugImageSegmentation = false;
        DebugRestrictedSearch = false;
        DebugCalibrationSheetBarycentricEstimate = false;
        DebugEllipseFinding = false;
        DebugManualEllipseSelection = false;
        DebugPointPairMatching = false;
        DebugPointPairMatchingSubsets = false;
        DebugEdgeFindingForEllipseDetection = false;
        DebugCalibrationSheetPlanarHomographyEstimate = false;
        DebugVoxelisation = false;
        DebugShow3DFlythough = false;
        DebugShow3DFlythoughImagePixelStep = 1;
        Debug = false;
        BlankOutputFilenameOnLoad = true;
        SkipStep = new boolean[numberofsteps];
        for (int i = 0; i < SkipStep.length; i++) SkipStep[i] = false;
        SkipStep[2] = true;
        CalibrationSheetKeepAspectRatioWhenPrinted = new JCheckBox("Preserve Aspect Ratio", true);
        PaperSizeIsCustom = new JCheckBox("Custom Paper Size", false);
        PaperOrientationIsPortrait = new JRadioButton("Portrait", true);
        PaperOrientationIsLandscape = new JRadioButton("Landscape", false);
        PaperOrientation = new ButtonGroup();
        PaperOrientation.add(PaperOrientationIsPortrait);
        PaperOrientation.add(PaperOrientationIsLandscape);
        OutputFileName = new JTextField();
        OutputFileName.setText(System.getProperty("user.home") + File.separatorChar + "output.stl");
        OutputObjectName = new JTextField();
        OutputObjectName.setText("");
        DebugSaveOutputImagesFolder = "";
        imagefiles = new DefaultListModel();
        calibrationpatterns = new DefaultComboBoxModel();
        AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees = 80;
        AlgorithmSettingMaxBundleAdjustmentNumberOfIterations = 100;
        AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment = 16;
        AlgorithmSettingEdgeStrengthThreshold = 20;
        AlgorithmSettingCalibrationSheetEdgeStrengthThreshold = 250;
        AlgorithmSettingEllipseValidityThresholdPercentage = 60;
        AlgorithmSettingVolumeSubDivision = 128;
        AlgorithmSettingResampledImageWidthForEllipseDetection = 1024;
        AlgorithmSettingPointPairMatchingDistanceThreshold = 100;
        AlgorithmSettingMinimumFoundValidEllipses = 4;
        AlgorithmSettingTextureMatchingAngleAccuracyInDegrees = 1;
        AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples = 20;
        AlgorithmSettingTextureMatchingMinimumSimilarityRange = 0.2;
        AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex = 0;
        AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees = 30;
        AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative = 4.0;
        AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity = 0.4;
        AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit = 0.1;
        AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment = false;
        CurrentCalibrationPatternIndexNumber = 0;
        CurrentPaperSizeIndexNumber = 0;
        PaperSizeList = new DefaultComboBoxModel();
        PaperSizeList.addElement(new Papersize("A4", 210, 297));
        PaperSizeList.addElement(new Papersize("US Letter", 215.9, 279.4));
        PaperSizeList.setSelectedItem(PaperSizeList.getElementAt(CurrentPaperSizeIndexNumber));
        DecimalFormat df = new DecimalFormat("###0.0");
        PaperMarginHorizontalmm = new JFormattedTextField(df);
        PaperMarginVerticalmm = new JFormattedTextField(df);
        ;
        PaperCustomSizeWidthmm = new JFormattedTextField(df);
        PaperCustomSizeHeightmm = new JFormattedTextField(df);
        PaperMarginHorizontalmm.setText("0");
        PaperMarginVerticalmm.setText("0");
        PaperCustomSizeWidthmm.setText("215.9");
        PaperCustomSizeHeightmm.setText("279.4");
        try {
            load();
        } catch (Exception e) {
            System.out.println("Error reading in parameters");
            System.out.println(e);
        }
    }

    public void load() throws IOException {
        int i;
        File file = new File(path);
        URL url = file.toURI().toURL();
        if (file.exists()) {
            Properties temp = new Properties();
            temp.load(url.openStream());
            if (temp.getProperty("OutputFileName") != null) OutputFileName.setText(temp.getProperty("OutputFileName"));
            if (temp.getProperty("OutputObjectName") != null) OutputObjectName.setText(temp.getProperty("OutputObjectName"));
            if (temp.getProperty("DebugSaveOutputImagesFolder") != null) {
                DebugSaveOutputImagesFolder = temp.getProperty("DebugSaveOutputImagesFolder");
                Debug = true;
            }
            if (temp.getProperty("SaveOnProgramWindowClose") != null) SaveOnProgramWindowClose = temp.getProperty("SaveOnProgramWindowClose").equals("true");
            if (temp.getProperty("SaveRestrictedSearchImageProperties") != null) SaveRestrictedSearchImageProperties = temp.getProperty("SaveRestrictedSearchImageProperties").equals("true");
            if (temp.getProperty("SaveOnProgramCancel") != null) SaveOnProgramCancel = temp.getProperty("SaveOnProgramCancel").equals("true");
            if (temp.getProperty("SaveOnProgramFinish") != null) SaveOnProgramFinish = temp.getProperty("SaveOnProgramFinish").equals("true");
            if (temp.getProperty("SaveCalibrationSheetProperties") != null) SaveCalibrationSheetProperties = temp.getProperty("SaveCalibrationSheetProperties").equals("true");
            if (temp.getProperty("SaveProcessedImageProperties") != null) SaveProcessedImageProperties = temp.getProperty("SaveProcessedImageProperties").equals("true");
            if (temp.getProperty("BlankOutputFilenameOnLoad") != null) BlankOutputFilenameOnLoad = temp.getProperty("BlankOutputFilenameOnLoad").equals("true");
            if (temp.getProperty("DebugImageOverlay") != null) DebugImageOverlay = temp.getProperty("DebugImageOverlay").equals("true");
            if (temp.getProperty("DebugImageSegmentation") != null) DebugImageSegmentation = temp.getProperty("DebugImageSegmentation").equals("true");
            if (temp.getProperty("DebugRestrictedSearch") != null) DebugRestrictedSearch = temp.getProperty("DebugRestrictedSearch").equals("true");
            if (temp.getProperty("DebugCalibrationSheetBarycentricEstimate") != null) DebugCalibrationSheetBarycentricEstimate = temp.getProperty("DebugCalibrationSheetBarycentricEstimate").equals("true");
            if (temp.getProperty("DebugEllipseFinding") != null) DebugEllipseFinding = temp.getProperty("DebugEllipseFinding").equals("true");
            if (temp.getProperty("DebugManualEllipseSelection") != null) DebugManualEllipseSelection = temp.getProperty("DebugManualEllipseSelection").equals("true");
            if (temp.getProperty("DebugPointPairMatching") != null) DebugPointPairMatching = temp.getProperty("DebugPointPairMatching").equals("true");
            if (temp.getProperty("DebugPointPairMatchingSubsets") != null) DebugPointPairMatchingSubsets = temp.getProperty("DebugPointPairMatchingSubsets").equals("true");
            if (temp.getProperty("DebugEdgeFindingForEllipseDetection") != null) DebugEdgeFindingForEllipseDetection = temp.getProperty("DebugEdgeFindingForEllipseDetection").equals("true");
            if (temp.getProperty("DebugCalibrationSheetPlanarHomographyEstimate") != null) DebugCalibrationSheetPlanarHomographyEstimate = temp.getProperty("DebugCalibrationSheetPlanarHomographyEstimate").equals("true");
            if (temp.getProperty("DebugVoxelisation") != null) DebugVoxelisation = temp.getProperty("DebugVoxelisation").equals("true");
            if (temp.getProperty("DebugShow3DFlythough") != null) DebugShow3DFlythough = temp.getProperty("DebugShow3DFlythough").equals("true");
            if (temp.getProperty("DebugIndividualTextureMatch") != null) DebugIndividualTextureMatch = temp.getProperty("DebugIndividualTextureMatch").equals("true");
            if (temp.getProperty("DebugMarchingTextureMatch") != null) DebugMarchingTextureMatch = temp.getProperty("DebugMarchingTextureMatch").equals("true");
            if (temp.getProperty("AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment") != null) AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment = temp.getProperty("AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment").equals("true");
            for (i = 0; i < SkipStep.length; i++) if (temp.getProperty("SkipStep" + Integer.toString(i + 1)) != null) SkipStep[i] = temp.getProperty("SkipStep" + Integer.toString(i + 1)).equals("true");
            if (temp.getProperty("CalibrationSheetKeepAspectRatioWhenPrinted") != null) CalibrationSheetKeepAspectRatioWhenPrinted.setSelected(temp.getProperty("CalibrationSheetKeepAspectRatioWhenPrinted").equals("true"));
            if (temp.getProperty("PaperSizeIsCustom") != null) PaperSizeIsCustom.setSelected(temp.getProperty("PaperSizeIsCustom").equals("true"));
            if (temp.getProperty("PaperOrientationIsPortrait") != null) {
                boolean set = temp.getProperty("PaperOrientationIsPortrait").equals("true");
                PaperOrientationIsPortrait = new JRadioButton(PaperOrientationIsPortrait.getText(), set);
                PaperOrientationIsLandscape = new JRadioButton(PaperOrientationIsLandscape.getText(), !set);
            }
            if (temp.getProperty("AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees") != null) {
                try {
                    AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees = Integer.valueOf(temp.getProperty("AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees - leaving as default: " + e);
                }
                if (AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees > 89) AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees = 89;
                if (AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees < 0) AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees = 0;
            }
            if (temp.getProperty("AlgorithmSettingMinimumFoundValidEllipses") != null) {
                try {
                    AlgorithmSettingMinimumFoundValidEllipses = Integer.valueOf(temp.getProperty("AlgorithmSettingMinimumFoundValidEllipses"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingMinimumFoundValidEllipses - leaving as default: " + e);
                }
                if (AlgorithmSettingMinimumFoundValidEllipses < 4) AlgorithmSettingMinimumFoundValidEllipses = 4;
            }
            if (temp.getProperty("AlgorithmSettingResampledImageWidthForEllipseDetection") != null) {
                try {
                    AlgorithmSettingResampledImageWidthForEllipseDetection = Integer.valueOf(temp.getProperty("AlgorithmSettingResampledImageWidthForEllipseDetection"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingResampledImageWidthForEllipseDetection - leaving as default: " + e);
                }
                if (AlgorithmSettingResampledImageWidthForEllipseDetection < 320) AlgorithmSettingResampledImageWidthForEllipseDetection = 320;
            }
            if (temp.getProperty("DebugShow3DFlythoughImagePixelStep") != null) {
                try {
                    DebugShow3DFlythoughImagePixelStep = Integer.valueOf(temp.getProperty("DebugShow3DFlythoughImagePixelStep"));
                } catch (Exception e) {
                    System.out.println("Error loading DebugShow3DFlythoughImagePixelStep - leaving as default: " + e);
                }
                if (DebugShow3DFlythoughImagePixelStep < 1) DebugShow3DFlythoughImagePixelStep = 1;
            }
            if (temp.getProperty("AlgorithmSettingPointPairMatchingDistanceThreshold") != null) {
                try {
                    AlgorithmSettingPointPairMatchingDistanceThreshold = Double.valueOf(temp.getProperty("AlgorithmSettingPointPairMatchingDistanceThreshold"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingPointPairMatchingDistanceThreshold - leaving as default: " + e);
                }
                if (AlgorithmSettingPointPairMatchingDistanceThreshold < 0) AlgorithmSettingPointPairMatchingDistanceThreshold = 1;
            }
            if (temp.getProperty("AlgorithmSettingEllipseValidityThresholdPercentage") != null) {
                try {
                    AlgorithmSettingEllipseValidityThresholdPercentage = Integer.valueOf(temp.getProperty("AlgorithmSettingEllipseValidityThresholdPercentage"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingEllipseValidityThresholdPercentage - leaving as default: " + e);
                }
                if (AlgorithmSettingEllipseValidityThresholdPercentage > 100) AlgorithmSettingEllipseValidityThresholdPercentage = 100;
                if (AlgorithmSettingEllipseValidityThresholdPercentage < 0) AlgorithmSettingEllipseValidityThresholdPercentage = 0;
            }
            if (temp.getProperty("AlgorithmSettingEdgeStrengthThreshold") != null) {
                try {
                    AlgorithmSettingEdgeStrengthThreshold = Integer.valueOf(temp.getProperty("AlgorithmSettingEdgeStrengthThreshold"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingEdgeStrengthThreshold - leaving as default: " + e);
                }
                if (AlgorithmSettingEdgeStrengthThreshold > 255) AlgorithmSettingEdgeStrengthThreshold = 255;
                if (AlgorithmSettingEdgeStrengthThreshold < 0) AlgorithmSettingEdgeStrengthThreshold = 0;
            }
            if (temp.getProperty("AlgorithmSettingCalibrationSheetEdgeStrengthThreshold") != null) {
                try {
                    AlgorithmSettingCalibrationSheetEdgeStrengthThreshold = Integer.valueOf(temp.getProperty("AlgorithmSettingCalibrationSheetEdgeStrengthThreshold"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingEdgeStrengthThreshold - leaving as default: " + e);
                }
                if (AlgorithmSettingCalibrationSheetEdgeStrengthThreshold > 255) AlgorithmSettingCalibrationSheetEdgeStrengthThreshold = 255;
                if (AlgorithmSettingCalibrationSheetEdgeStrengthThreshold < 0) AlgorithmSettingCalibrationSheetEdgeStrengthThreshold = 0;
            }
            if (temp.getProperty("AlgorithmSettingMaxBundleAdjustmentNumberOfIterations") != null) {
                try {
                    AlgorithmSettingMaxBundleAdjustmentNumberOfIterations = Integer.valueOf(temp.getProperty("AlgorithmSettingMaxBundleAdjustmentNumberOfIterations"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingMaxBundleAdjustmentNumberOfIterations - leaving as default: " + e);
                }
                if (AlgorithmSettingMaxBundleAdjustmentNumberOfIterations < 1) AlgorithmSettingMaxBundleAdjustmentNumberOfIterations = 1;
            }
            if (temp.getProperty("AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment") != null) {
                try {
                    AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment = Integer.valueOf(temp.getProperty("AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment - leaving as default: " + e);
                }
                if (AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment < 5) AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment = 5;
            }
            if (temp.getProperty("AlgorithmSettingVolumeSubDivision") != null) {
                try {
                    AlgorithmSettingVolumeSubDivision = Integer.valueOf(temp.getProperty("AlgorithmSettingVolumeSubDivision"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingVolumeSubDivision - leaving as default: " + e);
                }
                if (AlgorithmSettingVolumeSubDivision < 1) AlgorithmSettingVolumeSubDivision = 1;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples") != null) {
                try {
                    AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples = Integer.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples < 2) AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples = 2;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingMinimumSimilarityRange") != null) {
                try {
                    AlgorithmSettingTextureMatchingMinimumSimilarityRange = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingMinimumSimilarityRange"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingMinimumSimilarityRange - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingMinimumSimilarityRange < 0) AlgorithmSettingTextureMatchingMinimumSimilarityRange = 0;
                if (AlgorithmSettingTextureMatchingMinimumSimilarityRange > 1.0) AlgorithmSettingTextureMatchingMinimumSimilarityRange = 1.0;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingAngleAccuracyInDegrees") != null) {
                try {
                    AlgorithmSettingTextureMatchingAngleAccuracyInDegrees = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingAngleAccuracyInDegrees"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingAngleAccuracyInDegrees - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingAngleAccuracyInDegrees <= 0) AlgorithmSettingTextureMatchingAngleAccuracyInDegrees = 1;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex") != null) {
                try {
                    AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex < 0) AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex = 0;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees") != null) {
                try {
                    AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees < 0) AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees = 0;
                if (AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees > 90) AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees = 90;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative") != null) {
                try {
                    AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative < 0) AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative = 0;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity") != null) {
                try {
                    AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity < 0) AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity = 0;
                if (AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity > 1) AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity = 1;
            }
            if (temp.getProperty("AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit") != null) {
                try {
                    AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit = Double.valueOf(temp.getProperty("AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit"));
                } catch (Exception e) {
                    System.out.println("Error loading AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit - leaving as default: " + e);
                }
                if (AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit < 0) AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit = 0;
                if (AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit > 1) AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit = 1;
            }
            if (temp.getProperty("CurrentCalibrationPatternIndexNumber") != null) try {
                CurrentCalibrationPatternIndexNumber = Integer.valueOf(temp.getProperty("CurrentCalibrationPatternIndexNumber"));
            } catch (Exception e) {
                System.out.println("Error loading CurrentCalibrationPatternIndexNumber - leaving as default: " + e);
            }
            if (temp.getProperty("CurrentPaperSizeIndexNumber") != null) try {
                CurrentPaperSizeIndexNumber = Integer.valueOf(temp.getProperty("CurrentPaperSizeIndexNumber"));
            } catch (Exception e) {
                System.out.println("Error loading CurrentPaperSizeIndexNumber - leaving as default: " + e);
            }
            i = 0;
            while (temp.getProperty("ImageFileList" + Integer.toString(i)) != null) {
                String element = temp.getProperty("ImageFileList" + Integer.toString(i));
                boolean add = false;
                if (new File(element).exists()) {
                    add = true;
                    if (add) for (int j = 0; j < imagefiles.getSize(); j++) if (imagefiles.getElementAt(j).toString().equals(element)) add = false;
                }
                if (add) imagefiles.addElement(element);
                i++;
            }
            i = 0;
            while (temp.getProperty("CalibrationPatternFileList" + Integer.toString(i)) != null) {
                String element = temp.getProperty("CalibrationPatternFileList" + Integer.toString(i));
                boolean add = false;
                if (new File(element).exists()) {
                    add = !ImageFile.IsInvalid(element);
                    if (add) for (int j = 0; j < calibrationpatterns.getSize(); j++) if (calibrationpatterns.getElementAt(j).toString().equals(element)) add = false;
                }
                if (add) calibrationpatterns.addElement(element); else if (CurrentCalibrationPatternIndexNumber > i) CurrentCalibrationPatternIndexNumber--;
                i++;
            }
            i = 0;
            while ((temp.getProperty("PaperSizeNameList" + Integer.toString(i)) != null) && (temp.getProperty("PaperSizeWidthmmList" + Integer.toString(i)) != null) && (temp.getProperty("PaperSizeHeightmmList" + Integer.toString(i)) != null)) {
                Papersize element = new Papersize(temp.getProperty("PaperSizeNameList" + Integer.toString(i)), Double.valueOf(temp.getProperty("PaperSizeWidthmmList" + Integer.toString(i))), Double.valueOf(temp.getProperty("PaperSizeHeightmmList" + Integer.toString(i))));
                boolean add = true;
                for (int j = 0; j < PaperSizeList.getSize(); j++) if (((Papersize) PaperSizeList.getElementAt(j)).equals(element)) add = false;
                if (add) PaperSizeList.addElement(element);
                i++;
            }
            if ((CurrentCalibrationPatternIndexNumber < 0) || (CurrentCalibrationPatternIndexNumber >= calibrationpatterns.getSize())) CurrentCalibrationPatternIndexNumber = 0;
            if ((CurrentPaperSizeIndexNumber < 0) || (CurrentPaperSizeIndexNumber >= PaperSizeList.getSize())) CurrentPaperSizeIndexNumber = 0;
            if (temp.getProperty("PaperCustomSizeWidthmm") != null) {
                try {
                    PaperCustomSizeWidthmm.setText(temp.getProperty("PaperCustomSizeWidthmm"));
                } catch (Exception e) {
                    System.out.println("Error loading PaperCustomSizeWidthmm - leaving as default: " + e);
                }
                if (Double.valueOf(PaperCustomSizeWidthmm.getText()) < 1) PaperCustomSizeWidthmm.setText("1");
            }
            if (temp.getProperty("PaperCustomSizeHeightmm") != null) {
                try {
                    PaperCustomSizeHeightmm.setText(temp.getProperty("PaperCustomSizeHeightmm"));
                } catch (Exception e) {
                    System.out.println("Error loading PaperCustomSizeHeightmm- leaving as default: " + e);
                }
                if (Double.valueOf(PaperCustomSizeHeightmm.getText()) < 1) PaperCustomSizeHeightmm.setText("1");
            }
            if (temp.getProperty("PaperMarginHorizontalmm") != null) try {
                PaperMarginHorizontalmm.setText(temp.getProperty("PaperMarginHorizontalmm"));
            } catch (Exception e) {
                System.out.println("Error loading PaperMarginHorizontalmm - leaving as default: " + e);
            }
            if (temp.getProperty("PaperMarginVerticalmm") != null) try {
                PaperMarginVerticalmm.setText(temp.getProperty("PaperMarginVerticalmm"));
            } catch (Exception e) {
                System.out.println("Error loading PaperMarginVerticalmm - leaving as default: " + e);
            }
            SanityCheckMargins();
        }
        if (BlankOutputFilenameOnLoad) OutputFileName.setText("");
    }

    public void save() throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            File p = new File(file.getParent());
            if (!p.isDirectory()) {
                p.mkdirs();
            }
        }
        OutputStream output = new FileOutputStream(file);
        Properties temp = new Properties();
        temp.setProperty("OutputFileName", OutputFileName.getText());
        temp.setProperty("OutputObjectName", OutputObjectName.getText());
        temp.setProperty("PaperMarginHorizontalmm", PaperMarginHorizontalmm.getText());
        temp.setProperty("PaperMarginVerticalmm", PaperMarginVerticalmm.getText());
        temp.setProperty("PaperCustomSizeWidthmm", PaperCustomSizeWidthmm.getText());
        temp.setProperty("PaperCustomSizeHeightmm", PaperCustomSizeHeightmm.getText());
        temp.setProperty("DebugSaveOutputImagesFolder", DebugSaveOutputImagesFolder);
        temp.setProperty("SaveOnProgramFinish", String.valueOf(SaveOnProgramFinish));
        temp.setProperty("SaveRestrictedSearchImageProperties", String.valueOf(SaveRestrictedSearchImageProperties));
        temp.setProperty("SaveOnProgramWindowClose", String.valueOf(SaveOnProgramWindowClose));
        temp.setProperty("SaveOnProgramCancel", String.valueOf(SaveOnProgramCancel));
        temp.setProperty("SaveCalibrationSheetProperties", String.valueOf(SaveCalibrationSheetProperties));
        temp.setProperty("SaveProcessedImageProperties", String.valueOf(SaveProcessedImageProperties));
        temp.setProperty("BlankOutputFilenameOnLoad", String.valueOf(BlankOutputFilenameOnLoad));
        temp.setProperty("DebugImageOverlay", String.valueOf(DebugImageOverlay));
        temp.setProperty("DebugImageSegmentation", String.valueOf(DebugImageSegmentation));
        temp.setProperty("DebugRestrictedSearch", String.valueOf(DebugRestrictedSearch));
        temp.setProperty("DebugCalibrationSheetBarycentricEstimate", String.valueOf(DebugCalibrationSheetBarycentricEstimate));
        temp.setProperty("DebugEllipseFinding", String.valueOf(DebugEllipseFinding));
        temp.setProperty("DebugManualEllipseSelection", String.valueOf(DebugManualEllipseSelection));
        temp.setProperty("DebugShow3DFlythough", String.valueOf(DebugShow3DFlythough));
        temp.setProperty("DebugPointPairMatching", String.valueOf(DebugPointPairMatching));
        temp.setProperty("DebugPointPairMatchingSubsets", String.valueOf(DebugPointPairMatchingSubsets));
        temp.setProperty("DebugEdgeFindingForEllipseDetection", String.valueOf(DebugEdgeFindingForEllipseDetection));
        temp.setProperty("DebugCalibrationSheetPlanarHomographyEstimate", String.valueOf(DebugCalibrationSheetPlanarHomographyEstimate));
        temp.setProperty("DebugVoxelisation", String.valueOf(DebugVoxelisation));
        temp.setProperty("DebugIndividualTextureMatch", String.valueOf(DebugIndividualTextureMatch));
        temp.setProperty("DebugMarchingTextureMatch", String.valueOf(DebugMarchingTextureMatch));
        for (int i = 0; i < SkipStep.length; i++) temp.setProperty("SkipStep" + Integer.toString(i + 1), String.valueOf(SkipStep[i]));
        temp.setProperty("CalibrationSheetKeepAspectRatioWhenPrinted", String.valueOf(CalibrationSheetKeepAspectRatioWhenPrinted.isSelected()));
        temp.setProperty("PaperSizeIsCustom", String.valueOf(PaperSizeIsCustom.isSelected()));
        temp.setProperty("PaperOrientationIsPortrait", String.valueOf(PaperOrientationIsPortrait.isSelected()));
        for (int i = 0; i < imagefiles.getSize(); i++) temp.setProperty("ImageFileList" + Integer.toString(i), String.valueOf(imagefiles.getElementAt(i)));
        for (int i = 0; i < calibrationpatterns.getSize(); i++) temp.setProperty("CalibrationPatternFileList" + Integer.toString(i), String.valueOf(calibrationpatterns.getElementAt(i)));
        for (int i = 0; i < PaperSizeList.getSize(); i++) {
            Papersize element = (Papersize) PaperSizeList.getElementAt(i);
            temp.setProperty("PaperSizeNameList" + Integer.toString(i), element.Name);
            temp.setProperty("PaperSizeWidthmmList" + Integer.toString(i), String.valueOf(element.width));
            temp.setProperty("PaperSizeHeightmmList" + Integer.toString(i), String.valueOf(element.height));
        }
        temp.setProperty("DebugShow3DFlythoughImagePixelStep", String.valueOf(DebugShow3DFlythoughImagePixelStep));
        temp.setProperty("AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees", String.valueOf(AlgorithmSettingMaximumCameraAngleFromVerticalInDegrees));
        temp.setProperty("AlgorithmSettingMaxBundleAdjustmentNumberOfIterations", String.valueOf(AlgorithmSettingMaxBundleAdjustmentNumberOfIterations));
        temp.setProperty("AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment", String.valueOf(AlgorithmSettingStepsAroundCircleCircumferenceForEllipseEstimationInBundleAdjustment));
        temp.setProperty("AlgorithmSettingEdgeStrengthThreshold", String.valueOf(AlgorithmSettingEdgeStrengthThreshold));
        temp.setProperty("AlgorithmSettingCalibrationSheetEdgeStrengthThreshold", String.valueOf(AlgorithmSettingCalibrationSheetEdgeStrengthThreshold));
        temp.setProperty("AlgorithmSettingEllipseValidityThresholdPercentage", String.valueOf(AlgorithmSettingEllipseValidityThresholdPercentage));
        temp.setProperty("AlgorithmSettingVolumeSubDivision", String.valueOf(AlgorithmSettingVolumeSubDivision));
        temp.setProperty("AlgorithmSettingResampledImageWidthForEllipseDetection", String.valueOf(AlgorithmSettingResampledImageWidthForEllipseDetection));
        temp.setProperty("AlgorithmSettingPointPairMatchingDistanceThreshold", String.valueOf(AlgorithmSettingPointPairMatchingDistanceThreshold));
        temp.setProperty("AlgorithmSettingMinimumFoundValidEllipses", String.valueOf(AlgorithmSettingMinimumFoundValidEllipses));
        temp.setProperty("AlgorithmSettingTextureMatchingAngleAccuracyInDegrees", String.valueOf(AlgorithmSettingTextureMatchingAngleAccuracyInDegrees));
        temp.setProperty("AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples", String.valueOf(AlgorithmSettingTextureMatchingNthTriangularNumberOfSamples));
        temp.setProperty("AlgorithmSettingTextureMatchingMinimumSimilarityRange", String.valueOf(AlgorithmSettingTextureMatchingMinimumSimilarityRange));
        temp.setProperty("AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex", String.valueOf(AlgorithmSettingTextureMatchingMaxDistanceToSnapToTriangleVertex));
        temp.setProperty("AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees", String.valueOf(AlgorithmSettingTextureMatchingMinimumAverageAngleBetweenCameraAndPlaneInDegrees));
        temp.setProperty("AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative", String.valueOf(AlgorithmSettingTextureMatchingMinimumAbsoluteSecondDerivative));
        temp.setProperty("AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity", String.valueOf(AlgorithmSettingTextureMatchingMinimumValidMaxSimilarity));
        temp.setProperty("AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit", String.valueOf(AlgorithmSettingTextureMatchingMaxOverlapForSnapToFit));
        temp.setProperty("AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment", String.valueOf(AlgorithmSettingTextureMatchingManualSelectionOfInitialLineSegment));
        temp.setProperty("CurrentCalibrationPatternIndexNumber", String.valueOf(CurrentCalibrationPatternIndexNumber));
        temp.setProperty("CurrentPaperSizeIndexNumber", String.valueOf(CurrentPaperSizeIndexNumber));
        String comments = "Carapace Copier properties http://sourceforge.net/projects/carapace-copier/ - can be edited by hand but not recommended as elements may be reordered by the program\n";
        comments = comments + "Note that for boolean values, anything other than true (all in lowercase), will be evaluated as false \n";
        temp.store(output, comments);
    }

    public void SanityCheckMargins() {
        double w, h;
        if (PaperSizeIsCustom.isSelected()) {
            w = Double.valueOf(PaperCustomSizeWidthmm.getText());
            h = Double.valueOf(PaperCustomSizeHeightmm.getText());
        } else {
            Papersize current = (Papersize) PaperSizeList.getElementAt(CurrentPaperSizeIndexNumber);
            w = current.width;
            h = current.height;
        }
        if (Double.valueOf(PaperMarginVerticalmm.getText()) < 0) PaperMarginVerticalmm.setText("0");
        if (Double.valueOf(PaperMarginVerticalmm.getText()) > (h - 1)) PaperMarginVerticalmm.setText(String.valueOf(h - 1));
        if (Double.valueOf(PaperMarginHorizontalmm.getText()) < 0) PaperMarginHorizontalmm.setText("0");
        if (Double.valueOf(PaperMarginHorizontalmm.getText()) > (w - 1)) PaperMarginHorizontalmm.setText(String.valueOf(w - 1));
    }
}
