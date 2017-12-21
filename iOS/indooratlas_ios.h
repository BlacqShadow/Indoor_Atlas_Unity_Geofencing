//
//  indooratlas_ios.h
//  indooratlas_ios
//
//  Created by Rudra Kumar on 12/12/17.
//  Copyright © 2017 Rudra Kumar. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <IndoorAtlas/IALocationManager.h>

@interface indooratlas_ios : NSObject
- (void)init:(NSString *)gameObjectName apiKey:(NSString *)apiKey
                                     apiSecret:(NSString *)apiSecret
                            headingSensitivity:(double)headingSensitivity
                        orientationSensitivity:(double)orientationSensitivity;
- (void)close;
- (void)placeGeofence;
- (void)geoFenceTriggered:(NSString *)transition inArea:(NSString* )area;

@end
