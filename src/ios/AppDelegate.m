/*
 Licensed Materials - Property of IBM
 
 (C) Copyright 2017 IBM Corp.
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "AppDelegate.h"
#import <IBMMobileFirstPlatformFoundationHybrid/IBMMobileFirstPlatformFoundationHybrid.h>
#import <IBMMobileFirstPlatformFoundation/WLAnalytics.h>
#import "MainViewController.h"
#import "ibiVideo.h"

@implementation AppDelegate
    
    NSString* const MFP_INITIALIAZATION = @"WLInitSuccess";
    NSString* const OPEN_URL_COMPLETED = @"OpenURLCompleted";
    
-(void)playVideo {
    ibiVideo *video = [[ibiVideo alloc] init];
    [video play:nil];
}
    
- (BOOL)application:(UIApplication*)application didFinishLaunchingWithOptions:(NSDictionary*)launchOptions
    {
        if (NSClassFromString(@"CDVSplashScreen") == nil) {
            [[WL sharedInstance] showSplashScreen];
        }
        // By default splash screen will be automatically hidden once Worklight JavaScript framework is complete.
        // To override this behaviour set autoHideSplash property in initOptions.js to false and use WL.App.hideSplashScreen() API.
        [[WL sharedInstance] initializeWebFrameworkWithDelegate:self];
        
        __block __weak id observer =  [[NSNotificationCenter defaultCenter]addObserverForName:MFP_INITIALIAZATION object:nil queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification * note) {
            self.viewController = [[MainViewController alloc] init];
            self.viewController.startPage = [[WL sharedInstance] mainHtmlFilePath];
            [super application:application didFinishLaunchingWithOptions:launchOptions];
            [[NSNotificationCenter defaultCenter] removeObserver:observer name:MFP_INITIALIAZATION object:nil];
        }];
        
        [self playVideo];
        
        return YES;
    }
    
-(void)wlInitWebFrameworkDidCompleteWithResult:(WLWebFrameworkInitResult *)result
    {
        if ([result statusCode] == WLWebFrameworkInitResultSuccess) {
            [[WLAnalytics sharedInstance] addDeviceEventListener:NETWORK];
            [[WLAnalytics sharedInstance] addDeviceEventListener:LIFECYCLE];
            [[NSNotificationCenter defaultCenter] postNotificationName:MFP_INITIALIAZATION object:nil];
        } else {
            [self wlInitDidFailWithResult:result];
        }
    }
    
-(void)wlInitDidFailWithResult:(WLWebFrameworkInitResult *)result
    {
        UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"ERROR"
                                                            message:[result message]
                                                           delegate:self
                                                  cancelButtonTitle:@"OK"
                                                  otherButtonTitles:nil];
        [alertView show];
    }
    
    
- (void)applicationWillResignActive:(UIApplication *)application
    {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    }
    
- (void)applicationDidEnterBackground:(UIApplication *)application
    {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }
    
- (void)applicationWillEnterForeground:(UIApplication *)application
    {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
        //wi 116840 - Adding a post notification on mfp_intialization after openurl is executed and an observer notification is received.
        //This is to handle warm-start correctly
        __block __weak id observer = [[NSNotificationCenter defaultCenter]addObserverForName:OPEN_URL_COMPLETED object:nil queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification * note) {
            [[NSNotificationCenter defaultCenter] postNotificationName:MFP_INITIALIAZATION object:nil];
            [[NSNotificationCenter defaultCenter] removeObserver:observer name:OPEN_URL_COMPLETED object:nil];
        }];
        
    }
    
- (void)applicationDidBecomeActive:(UIApplication *)application
    {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }
    
- (void)applicationWillTerminate:(UIApplication *)application
    {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
    
    //wi 116840 - The below changes is to fix cold-start in handleopenurl. Only after the MFP_INITIALIZATION observer is returned, the handleopenurl plugin is called.
    //This ensures that cold-start behaviour does not face any issues.
    // After this, an observer for open_url_completed is initiated. This ensures that in "applicationWillEnterForeground", the action to post notification is completed after openurl is executed.
- (BOOL)application:(UIApplication*)application openURL:(NSURL*)url sourceApplication:(NSString*)sourceApplication annotation:(id)annotation
    {
        if (!url) {
            return NO;
        }
        __block __weak id observer = [[NSNotificationCenter defaultCenter]addObserverForName:MFP_INITIALIAZATION object:nil queue:[NSOperationQueue mainQueue] usingBlock:^(NSNotification * note) {
            [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:CDVPluginHandleOpenURLNotification object:url]];
            [[NSNotificationCenter defaultCenter] removeObserver:observer name:MFP_INITIALIAZATION object:nil];
        }];
        [[NSNotificationCenter defaultCenter] postNotificationName:OPEN_URL_COMPLETED object:nil];
        return YES;
    }
    
    @end
