package com.example.krishna.mymap;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainActivity extends FragmentActivity {

    public static String API_KEY="AIzaSyAqw3ox2Kr5fNKtjTSoSmkADp3x2sxzgBg";

    private static final LatLng LOWER_MANHATTAN = new LatLng(40.722543,
            -73.998585);
    private static final LatLng TIMES_SQUARE = new LatLng(40.7577, -73.9857);
    private static final LatLng BROOKLYN_BRIDGE = new LatLng(40.7057, -73.9964);

    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();

    }

    private void setUpMapIfNeeded() {
        // check if we have got the googleMap already
        if (googleMap == null) {
            Toast.makeText(this, "getting map reference", Toast.LENGTH_SHORT).show();
            googleMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
            if (googleMap != null) {
                addLines();
            }
        }
    }

    private void addLines() {

        ArrayList<MyMapPoints> lats=getMapPoint();

        addMarketPointsOnMap(lats.get(0), lats.get(1));
        addMarketPointsOnMap(lats.get(1), lats.get(2));
        addMarketPointsOnMap(lats.get(2), lats.get(3));
        addMarketPointsOnMap(lats.get(3), lats.get(4));

        GetPlaces getPlaces=new GetPlaces(this);
        getPlaces.execute();

//        googleMap
//                .addPolyline((new PolylineOptions())
////                        .add(TIMES_SQUARE, BROOKLYN_BRIDGE, LOWER_MANHATTAN,
////                                TIMES_SQUARE)
//                        .add(lats.get(0).getLatLng(), lats.get(1).getLatLng(), lats.get(2).getLatLng(), lats.get(3).getLatLng(), lats.get(4).getLatLng())
//                        .width(5).color(Color.BLUE)
//                        .geodesic(true));
        // move camera to zoom on map
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lats.get(0).getLatLng(),
                13));
        Toast.makeText(this, "adding Lines", Toast.LENGTH_SHORT).show();
    }

    private void addMarketPointsOnMap(MyMapPoints src, MyMapPoints dest){
        googleMap.addMarker(new MarkerOptions().position(src.getLatLng()).title(src.getLocation()));
        AsyncTask task=new connectAsyncTask(this, makeURL(src.getLat(), src.getLng(), dest.getLat(), dest.getLng()));
        task.execute();
    }

    public ArrayList<MyMapPoints> getMapPoint(){
        ArrayList<MyMapPoints> pnts=new ArrayList<>();
        pnts.add(new MyMapPoints("A",17.445876, 78.348481,"Red"));
        pnts.add(new MyMapPoints("B", 17.430657, 78.342477,"Red"));
        pnts.add(new MyMapPoints("C", 17.435240, 78.339851,"Red"));
        pnts.add(new MyMapPoints("D" ,17.460552, 78.357919,"Green"));
        pnts.add(new MyMapPoints("E",17.463335, 78.373529,"Yellow"));
        return pnts;
    }

    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        return urlString.toString();
    }

    public void drawPath(String  result) {

        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);

                Polyline line = googleMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                        .width(2)
                        .color(Color.BLUE).geodesic(true));
            }

        }
        catch (JSONException e) {

        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    private static class connectAsyncTask extends AsyncTask<Object, Void, String> {
        private ProgressDialog progressDialog;
        private MainActivity context;
        String url;
        connectAsyncTask(MainActivity context, String urlPass){
            this.context = context;
            url = urlPass;
        }
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
//            progressDialog = new ProgressDialog(context);
//            progressDialog.setMessage("Fetching route, Please wait...");
//            progressDialog.setIndeterminate(true);
//            progressDialog.show();
        }
        @Override
        protected String doInBackground(Object... params) {
            Log.e("connectAsyncTask", "doInBackground (Line:192)  :  started....");
            JSONParser jParser = new JSONParser();
            Log.e("connectAsyncTask", "doInBackground (Line:194)  :  jparser....");
            String json = jParser.getJSONFromUrl(url);
            Log.e("connectAsyncTask", "doInBackground (Line:196)  :  json....");
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
//            progressDialog.hide();
            Log.d("connectAsyncTask", "onPostExecute (Line:201) :"+result);
            Toast.makeText(context, "added map points", Toast.LENGTH_SHORT).show();
            if(result!=null){
                context.drawPath(result);
            }
        }
    }

}

class PlaceFind {
    private String id;
    private String icon;
    private String name;
    private String vicinity;
    private Double latitude;
    private Double longitude;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVicinity() {
        return vicinity;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    static PlaceFind jsonToPontoReferencia(JSONObject pontoReferencia) {
        try {
            PlaceFind result = new PlaceFind();
            JSONObject geometry = (JSONObject) pontoReferencia.get("geometry");
            JSONObject location = (JSONObject) geometry.get("location");
            result.setLatitude((Double) location.get("lat"));
            result.setLongitude((Double) location.get("lng"));
            result.setIcon(pontoReferencia.getString("icon"));
            result.setName(pontoReferencia.getString("name"));
            result.setVicinity(pontoReferencia.getString("vicinity"));
            result.setId(pontoReferencia.getString("id"));
            return result;
        } catch (JSONException ex) {
            Logger.getLogger(Place.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Place{" + "id=" + id + ", icon=" + icon + ", name=" + name + ", latitude=" + latitude + ", longitude=" + longitude + '}';
    }

}

class GetPlaces extends AsyncTask<Void, Void, Void>{

    private ProgressDialog dialog;
    private Context context;
    private String[] placeName;
    private String[] imageUrl;

    public GetPlaces(Context context) {
        // TODO Auto-generated constructor stub
        this.context = context;
    }

    @Override
    protected void onPostExecute(Void result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
        dialog.dismiss();

    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
        dialog = new ProgressDialog(context);
        dialog.setCancelable(true);
        dialog.setMessage("Loading..");
        dialog.isIndeterminate();
        dialog.show();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        // TODO Auto-generated method stub
        PlacesService service = new PlacesService();
        List<PlaceFind> findPlaces = service.findPlaces(17.445876, 78.348481,"hospital");  // hospiral for hospital
        // atm for ATM

        placeName = new String[findPlaces.size()];
        imageUrl = new String[findPlaces.size()];

        for (int i = 0; i < findPlaces.size(); i++) {

            PlaceFind placeDetail = findPlaces.get(i);
            placeDetail.getIcon();

            System.out.println(  placeDetail.getName());
            placeName[i] =placeDetail.getName();

            imageUrl[i] =placeDetail.getIcon();

            Log.e("GetPlaces", "doInBackground (Line:364) :"+placeDetail.getName());

        }
        return null;
    }

}

class PlacesService {

    public List<PlaceFind> findPlaces(double latitude, double longitude,String placeSpacification)
    {

        String urlString = makeUrl(latitude, longitude,placeSpacification);


        try {
            String json = getJSON(urlString);

            System.out.println(json);
            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray("results");

            Log.e("PlacesService", "findPlaces (Line:387) :"+array);

            ArrayList<PlaceFind> arrayList = new ArrayList<PlaceFind>();
            for (int i = 0; i < array.length(); i++) {
                try {
                    PlaceFind place = PlaceFind.jsonToPontoReferencia((JSONObject) array.get(i));

                    Log.v("Places Services ", ""+place);


                    arrayList.add(place);
                } catch (Exception e) {
                }
            }
            return arrayList;
        } catch (JSONException ex) {
            Logger.getLogger(PlacesService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    //https://maps.googleapis.com/maps/api/place/search/json?location=28.632808,77.218276&radius=500&types=atm&sensor=false&key=apikey
    private String makeUrl(double latitude, double longitude,String place) {
        StringBuilder urlString = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?");

        if (place.equals("")) {
            urlString.append("&location=");
            urlString.append(Double.toString(latitude));
            urlString.append(",");
            urlString.append(Double.toString(longitude));
            urlString.append("&radius=5000");
            //   urlString.append("&types="+place);
            urlString.append("&sensor=false&key=" + MainActivity.API_KEY);
        } else {
            urlString.append("&location=");
            urlString.append(Double.toString(latitude));
            urlString.append(",");
            urlString.append(Double.toString(longitude));
            urlString.append("&radius=5000");
            urlString.append("&types="+place);
            urlString.append("&sensor=false&key=" + MainActivity.API_KEY);
        }


        Log.e("PlacesService", "makeUrl (Line:430) :"+urlString);
        return urlString.toString();
    }

    protected String getJSON(String url) {
        return getUrlContents(url);
    }

    private String getUrlContents(String theUrl)
    {
        StringBuilder content = new StringBuilder();

        try {
            URL url = new URL(theUrl);
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 8);
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                content.append(line + "\n");
            }

            bufferedReader.close();
        }

        catch (Exception e)
        {

            e.printStackTrace();

        }

        return content.toString();
    }
}

class JSONParser {

    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";
    // constructor
    public JSONParser() {
    }
    public String getJSONFromUrl(String url) {

        // Making HTTP request
        try {
            // defaultHttpClient
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            json = sb.toString();

            Log.e("JSONParser", "getJSONFromUrl (Line:250) :"+json);
            is.close();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
        return json;

    }


}


class MyMapPoints{
//    Location A,17.445876, 78.348481,Red
//    Location B, 17.430657, 78.342477,Red
//    Location C, 17.435240, 78.339851,Red
//    Location D ,17.460552, 78.357919,Green
//    Location E,17.463335, 78.373529,Yellow

    private String location;
    private double lat;
    private double lng;
    private String clr;

    MyMapPoints(String location, double lat, double lng, String clr) {
        this.location = location;
        this.lat = lat;
        this.lng = lng;
        this.clr = clr;
    }

    public LatLng getLatLng(){
        return new LatLng(lat, lng);
    }


    public String getClr() {
        return clr;
    }

    public void setClr(String clr) {
        this.clr = clr;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }


}

