package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public SwipeRefreshLayout swipeRefresh;
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    public DrawerLayout drawerLayout;
    private Button navButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        weatherLayout=(ScrollView) findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);//城市名
        titleUpdateTime=(TextView)findViewById(R.id.title_update_time);//更新时间
        degreeText=(TextView)findViewById(R.id.degree_text);//当前气温
        weatherInfoText=(TextView)findViewById(R.id.weather_info_text);//天气概况
        forecastLayout=(LinearLayout) findViewById(R.id.forecast_layout);//未来天气的内容
        aqiText=(TextView)findViewById(R.id.aqi_text);//AQI指数
        pm25Text=(TextView)findViewById(R.id.pm25_text);//PM 2.5指数
        comfortText=(TextView)findViewById(R.id.comfort_text);//舒适度
        carWashText=(TextView)findViewById(R.id.car_wash_text);//洗车指数
        sportText=(TextView)findViewById(R.id.sport_text);//运动建议
        bingPicImg=(ImageView) findViewById(R.id.bing_pic_img);
        drawerLayout=(DrawerLayout) findViewById(R.id.drawer_layout);//滑动菜单
        navButton=(Button) findViewById(R.id.nav_button); //切换城市按钮
        swipeRefresh=(SwipeRefreshLayout) findViewById(R.id.swipe_refresh);//下拉刷新
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);//设置下拉刷新进度条的颜色

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);
        final String weatherId;
        if(weatherString!=null){
            Weather weather= Utility.handleWeatherResponse(weatherString);
            weatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            weatherId=getIntent().getStringExtra("weather_id");//从Intent中取出天气id
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);//从服务器请求天气数据
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {//下拉刷新的监听器
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });

        String bingPic=prefs.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }
    public void requestWeather(final String weatherId){
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+"&key=cf5eecb8fdde4d2fb6a5f6da9111b309";
        HttpUtil.sendOkHttpRequst(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               e.printStackTrace();
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                       swipeRefresh.setRefreshing(false);
                   }
               });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);

                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                            swipeRefresh.setRefreshing(false);
                        }
                    }
                });
            }
        });
        loadBingPic();
    }
    /*
    处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather){
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"℃";
        String weatherInfo=weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dataText=(TextView) view.findViewById(R.id.date_text);
            TextView infoText=(TextView) view.findViewById(R.id.info_text);
            TextView maxText=(TextView) view.findViewById(R.id.max_text);
            TextView minText=(TextView) view.findViewById(R.id.min_text);
            dataText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);

        }
        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);

        }
        String comfort="舒适度: "+weather.suggestion.comfort.info;
        String carwash="洗车指数: "+weather.suggestion.carWash.info;
        String sport="运动建议: "+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carwash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent=new Intent(this, AutoUpdateService.class);//启动服务
        startService(intent);
    }
    private void loadBingPic(){
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequst(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}
