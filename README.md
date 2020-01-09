
***SplashScreen Video  for Cordova Application***   
  
  

## **1. Introduction**

  
`For many projects based of Cordova, we need to add video SplashScreen, I shared with you this tutorial`  
  

## **2. Supported Platforms**

  
- Android  
  
- iPhone  
  
- iPad  
  

## **3 . Screenshots**

  

## **4. Installation**

  
- Download Cordova project : cordova create Your_Cordova_Project  
  
- Install plugin SplashScreen : cordova plugin add cordova-plugin-splashscreen  
  
     => Recommanded 5.0.3 : Comptabile with iPhoneX, iPhoneXS...   
            cordova plugin add cordova-plugin-splashscreen@5.0.3  
  
- Add Platforms :  
  * cordova platform add android  
      
  * cordova platform add ios  
      
- For platform Android  
  
  + Copy of content from file cordova-plugin-splashscreenvideo/src/android/SplashScreenVideo.java,  
    and past in your project: Your_Cordova_Project/platforms/android/app/src/main/java/org/apache/cordova/splashscreen/SplashScreen.java  
  
  + Create new folder in your project "raw" : Your_Cordova_Project/platforms/android/app/src/main/res/raw  
      
  + Add video animation, int this folder (raw) with name "start_animation.mp4"  
      
  + Run your project
   
- For platform iOS  
  
  + Open Your Project with Xcode  
      
  + Copy two files (ibiVideo.h, ibiVideo.m) from cordova-plugin-splashscreenvideo/src/ios/  
    to iOSNativePlatforms/Classes        
      
    => Copy reference and select checkbox "Copy Items if needed" and "Crerate folder reference" and "Add to targets"
      
  + Copy of content from cordova-plugin-splashscreenvideo/src/ios/AppDelegate.m and Past in iOSNativePlatforms/Classes/AppDelegate.m  
      
  + Copy source of video animation "start_animation.mp4" to iOSNativePlatforms/Resources   
    and check checkbox  "Copy items if needed" and "Create folder references" and "Add to targets"
       
  + Run Your Project  
      

## 5. Demo
