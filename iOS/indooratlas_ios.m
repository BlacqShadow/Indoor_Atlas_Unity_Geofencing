//
//  indooratlas_ios.m
//  indooratlas_ios
//
//  Created by Rudra Kumar on 12/12/17.
//  Copyright © 2017 Rudra Kumar. All rights reserved.
//

#import "indooratlas_ios.h"
#import <IndoorAtlas/IndoorAtlas.h>


@interface indooratlas_ios () <IALocationManagerDelegate>
@property (nonatomic, strong) IALocationManager *manager;
@property (nonatomic, copy) NSString *key;
@property (nonatomic, copy) NSString *secret;
@property (nonatomic, copy) NSString *gameObject;
@property (nonatomic, strong) IAGeofence *geofence;
@end
extern void UnitySendMessage(const char *, const char *, const char *);
@implementation indooratlas_ios
- (void)init:(NSString *)gameObjectName apiKey:(NSString *)apiKey apiSecret:(NSString *)apiSecret headingSensitivity:(double)headingSensitivity orientationSensitivity:(double)orientationSensitivity
{
    // Create IALocationManager and point delegate to receiver
    self.manager = [IALocationManager sharedInstance];
    self.manager.delegate = self;
    self.manager.headingFilter = (CLLocationDegrees)headingSensitivity;
    self.manager.attitudeFilter = (CLLocationDegrees)orientationSensitivity;
    self.gameObject = gameObjectName;
    
    // Set IndoorAtlas API key and secret
    [self.manager setApiKey:apiKey andSecret:apiSecret];
    
    // Request location updates
    [self.manager startUpdatingLocation];
    
    
}

- (void)close
{
    [self.manager stopUpdatingLocation];
}

- (NSString *)dictionaryToJSONString:(NSDictionary*)dictionary
{
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dictionary options:0 error:&error];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    if (error != nil) {
        NSException* serializationError = [NSException
                                           exceptionWithName:@"unableToSerializeDictionaryException"
                                           reason:@"Unable to serialize NSDictionary"
                                           userInfo:nil];
        [serializationError raise];
    }
    return jsonString;
}

- (void)indoorLocationManager:(IALocationManager*)manager didUpdateLocations:(NSArray*)locations
{
    (void)manager;
    CLLocation *l = [(IALocation*)locations.lastObject location];
    NSDictionary *locationDictionary = @{
                                         @"accuracy": @(l.horizontalAccuracy),
                                         @"altitude": @(l.altitude),
                                         @"bearing": @(l.course),
                                         @"floorLevel": @(l.floor.level),
                                         @"hasFloorLevel": @(l.floor != nil),
                                         @"latitude": @(l.coordinate.latitude),
                                         @"longitude": @(l.coordinate.longitude),
                                         @"timestamp": @([l.timestamp timeIntervalSince1970])
                                         };
    
    NSString *jsonString = [self dictionaryToJSONString:locationDictionary];
    UnitySendMessage([self.gameObject UTF8String], "onLocationChanged", [jsonString UTF8String]);
    // Place Geofences
    [self placeGeofence];
    
}

/************************************ GEOFENCING CODE START *****************************************************/
// Geofence Listener

- (void)placeGeofence
{
   
    NSMutableArray<NSNumber*> *edges = [NSMutableArray array];
    
    [edges addObject:[NSNumber numberWithDouble:-37.84811769]];
    [edges addObject:[NSNumber numberWithDouble:145.11430576]];
    [edges addObject:[NSNumber numberWithDouble:-37.84802129]];
    [edges addObject:[NSNumber numberWithDouble:145.11431266]];
    [edges addObject:[NSNumber numberWithDouble:-37.84792601]];
    [edges addObject:[NSNumber numberWithDouble:145.11432588]];
    [edges addObject:[NSNumber numberWithDouble:-37.84792520]];
    [edges addObject:[NSNumber numberWithDouble:145.11433971]];
    [edges addObject:[NSNumber numberWithDouble:-37.84802397]];
    [edges addObject:[NSNumber numberWithDouble:145.11433594]];
    [edges addObject:[NSNumber numberWithDouble:-37.84811399]];
    [edges addObject:[NSNumber numberWithDouble:145.11432856]];
    
    self.geofence = [IAPolygonGeofence polygonGeofenceWithIdentifier:@"Area 1" andFloor:nil edges:edges];

    [self.manager startMonitoringForGeofence:self.geofence];
    

}












/************************************ GEOFENCING CODE END ******************************************************/
- (void)indoorLocationManager:(IALocationManager *)manager didUpdateAttitude:(nonnull IAAttitude *)newAttitude
{
    (void)manager;
    NSDictionary *attitude = @{
                               @"x": @(newAttitude.quaternion.x),
                               @"y": @(newAttitude.quaternion.y),
                               @"z": @(newAttitude.quaternion.z),
                               @"w": @(newAttitude.quaternion.w),
                               @"timestamp": @((long)([newAttitude.timestamp timeIntervalSince1970] * 1000.0))
                               };
    NSString *jsonString = [self dictionaryToJSONString:attitude];
    UnitySendMessage([self.gameObject UTF8String], "onOrientationChange", [jsonString UTF8String]);
}

- (void)indoorLocationManager:(nonnull IALocationManager *)manager didUpdateHeading:(nonnull IAHeading *)newHeading
{
    (void)manager;
    NSDictionary *heading = @{
                              @"heading": @(newHeading.trueHeading),
                              @"timestamp": @((long)([newHeading.timestamp timeIntervalSince1970] * 1000.0))
                              };
    NSString *jsonString = [self dictionaryToJSONString:heading];
    UnitySendMessage([self.gameObject UTF8String], "onHeadingChanged", [jsonString UTF8String]);
}

- (void)indoorLocationManager:(nonnull IALocationManager *)manager statusChanged:(nonnull IAStatus *)status
{
    (void)manager;
    NSDictionary *statusDictionary = @{@"status": @(status.type)};
    NSString *jsonString = [self dictionaryToJSONString:statusDictionary];
    UnitySendMessage([self.gameObject UTF8String], "onStatusChanged", [jsonString UTF8String]);
}

- (NSString*)regionToJSONString:(IARegion *)region
{
    NSDictionary *regionDictionary = @{
                                       @"id":        region.identifier,
                                       @"name":      region.name,
                                       @"type":      @(region.type),
                                       @"timestamp": region.timestamp ? @([region.timestamp timeIntervalSince1970]) : @(-1),
                                       };
    return [self dictionaryToJSONString:regionDictionary];
}

- (void)indoorLocationManager:(IALocationManager *)manager didEnterRegion:(IARegion *)region
{
    (void)manager;
    if (region.type == kIARegionTypeGeofence) {
        [self geoFenceTriggered:@"ENTER" inArea:region.name];
        /*self.inside = true;
        
        if (self.overlay) {
            [self.map removeOverlay:self.overlay];
            [self.map addOverlay:self.overlay];
        }*/
        
    }
    NSString *jsonString = [self regionToJSONString:region];
    UnitySendMessage([self.gameObject UTF8String], "onEnterRegion", [jsonString UTF8String]);
}

- (void)indoorLocationManager:(IALocationManager *)manager didExitRegion:(IARegion *)region
{
    (void)manager;
    if (region.type == kIARegionTypeGeofence) {
        [self geoFenceTriggered:@"EXIT" inArea:region.name];
        /*self.inside = true;
         
         if (self.overlay) {
         [self.map removeOverlay:self.overlay];
         [self.map addOverlay:self.overlay];
         }*/
        
    }
    NSString *jsonString = [self regionToJSONString:region];
    UnitySendMessage([self.gameObject UTF8String], "onExitRegion", [jsonString UTF8String]);
}

- (void)geoFenceTriggered:(NSString *)transition inArea:(NSString* )area
{
    NSDictionary *geoFenceDictionary = @{
                                         @"area": area,
                                         @"transition": transition
                                         };
    
    NSString *jsonString = [self dictionaryToJSONString:geoFenceDictionary];
    UnitySendMessage([self.gameObject UTF8String], "onGeofencesTriggered", [jsonString UTF8String]);
}

@end
