package com.example.potholedetectionapp;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.example.potholedetectionapp.libsvm.svm_model;
import com.example.potholedetectionapp.libsvm.svm;
import com.example.potholedetectionapp.libsvm.svm_node;
import com.example.potholedetectionapp.libsvm.svm_parameter;
import com.example.potholedetectionapp.libsvm.svm_problem;


public class SVM {
    static int record_count = 1;
    static int feature_count = 6;

    // Feature[feature_index][feature_value]
//    static double[][] features = new double[record_count][feature_count];

    /*
    MAX: 1904.0
    MIN: -2822.0
    MAX: 4762.0
    MIN: -5265.0
    MAX: 3860.0
    MIN: 207.0
    MAX: 1819.0
    MIN: -2888.0
    MAX: 3719.0
    MIN: -2538.0
    MAX: 4729.0
    MIN: 180.0
     */
    static double [] feature_max = { 1904, 4762, 3860, 1819, 3719, 4729 };
    static double [] feature_min = { -2822, -5265, 207, -2888, -2538, 180 };


    @RequiresApi(api = Build.VERSION_CODES.N)
    public static svm_model load_model(Context contextApp)
    {
        //File file = null;
        try {
//            System.out.println(System.getProperty("user.dir"));
//            String path = System.getProperty("user.dir") + "\\src\\com\\company\\svm_model";
//            String path = "\\svm_model";
//            System.out.println(path);

            return svm_load_model("test.txt", contextApp);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static svm_model svm_load_model(String model_file_name, Context contextApp) throws IOException
    {
        //return svm_load_model(new BufferedReader(new FileReader(model_file_name)));
        return svm.svm_load_model(new BufferedReader(new InputStreamReader(contextApp.getAssets().open(model_file_name))));//contextApp.getAssets()
    }


    public static double[] svmPredict(double[][] xtest, svm_model model) {

        double[] yPred = new double[xtest.length];

        for (int k = 0; k < xtest.length; k++) {
            double[] fVector = xtest[k];

            svm_node[] nodes = new svm_node[fVector.length];

            for (int i = 0; i < fVector.length; i++) {
                svm_node node = new svm_node();
                node.index = i;
                node.value = fVector[i];
                nodes[i] = node;
            }

            int totalClasses = 3;
            int[] labels = new int[totalClasses];
            svm.svm_get_labels(model, labels);

            double[] prob_estimates = new double[totalClasses];
            yPred[k] = svm.svm_predict_probability(model, nodes, prob_estimates);
        }

        return yPred;

    }


    public static void normalize_features(double[][] features) {
        // static double[][] features = new double[record_count][feature_count];
        for (int i = 0; i<record_count; i++){
            for (int k = 0; k<feature_count; k++){
                features[i][k] = normalize(features[i][k],feature_min[k], feature_max[k],0,1);
                System.out.println("F"+ k + ": " + features[i][k]);
            }
        }
    }


    /**
     * Normalize x.
     * @param x The value to be normalized.
     * @return The result of the normalization.
     */
    public static double normalize(double x, double dataLow, double dataHigh, double normalizedLow, double normalizedHigh) {
        return ((x - dataLow)
                / (dataHigh - dataLow))
                * (normalizedHigh - normalizedLow) + normalizedLow;
    }

}
