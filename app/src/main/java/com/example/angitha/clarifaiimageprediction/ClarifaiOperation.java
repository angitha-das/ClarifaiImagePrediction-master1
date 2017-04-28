package com.example.angitha.clarifaiimageprediction;

import android.os.AsyncTask;
import android.view.View;

import java.util.List;

import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

/**
 * Created by angitha on 27/4/17.
 */

//    android.os.AsyncTask<Params, Progress, Result>
class ClarifaiOperation extends AsyncTask<Object, Object, List<ClarifaiOutput<Concept>>> {
    ClarifaiClient client1;
    byte[] imageCaptured1;
    public AsyncResponse delegate = null;

    public ClarifaiOperation(ClarifaiClient client, byte[] imageCaptured) {
        client1  = client;
        imageCaptured1 = imageCaptured;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<ClarifaiOutput<Concept>> doInBackground(Object... params) {

        //predict
        final List<ClarifaiOutput<Concept>> predictionResults =
                client1.getDefaultModels().generalModel() // You can also do Clarifai.getModelByID("id") to get custom models
                        .predict()
                        .withInputs(
                                ClarifaiInput.forImage(ClarifaiImage.of(imageCaptured1))
                        )
                        .executeSync() // optionally, pass a ClarifaiClient parameter to override the default client instance with another one
                        .get();
        return predictionResults;
    }

    @Override
    protected void onPostExecute(List<ClarifaiOutput<Concept>> s) {
        //super.onPostExecute(s);
        delegate.processFinish(s);
    }

    public interface AsyncResponse {
        void processFinish(List<ClarifaiOutput<Concept>> s);
    }

}