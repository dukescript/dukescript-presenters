
package com.dukescript.presenters.robovm;

/*
 * #%L
 * DukeScript Presenter for iOS - a library from the "DukeScript Presenters" project.
 * Visit http://dukescript.com for support and commercial license.
 * %%
 * Copyright (C) 2015 Eppleton IT Consulting
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import net.java.html.geo.OnLocation;
import org.netbeans.html.geo.spi.GLProvider;
import org.openide.util.lookup.ServiceProvider;
import org.robovm.apple.corelocation.CLAuthorizationStatus;
import org.robovm.apple.corelocation.CLBeacon;
import org.robovm.apple.corelocation.CLBeaconRegion;
import org.robovm.apple.corelocation.CLHeading;
import org.robovm.apple.corelocation.CLLocation;
import org.robovm.apple.corelocation.CLLocationManager;
import org.robovm.apple.corelocation.CLLocationManagerDelegateAdapter;
import org.robovm.apple.corelocation.CLRegion;
import org.robovm.apple.corelocation.CLRegionState;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSError;

/** Implements geolocation services for RoboVM. Use {@link OnLocation} annotation
 * to access this implementation.
 */
@ServiceProvider(service = GLProvider.class)
public final class RoboVMGeo extends GLProvider<CLLocation,RoboVMGeo.Adapter> {
    @Override
    protected Adapter start(Query query) {
        if (
            CLLocationManager.getAuthorizationStatus() == CLAuthorizationStatus.Denied
        ) {
            return null;
        }
        CLLocationManager cl = new CLLocationManager();
        if (query.isHighAccuracy()) {
            cl.setDesiredAccuracy(10.0);
        }
        Adapter del = new Adapter(query, cl);
        cl.setDelegate(del);
        cl.startUpdatingLocation();
        return del;
    }

    @Override
    protected void stop(Adapter watch) {
        watch.m.stopUpdatingLocation();
    }

    @Override
    protected double latitude(CLLocation coords) {
        return coords.getCoordinate().getLatitude();
    }

    @Override
    protected double longitude(CLLocation coords) {
        return coords.getCoordinate().getLongitude();
    }

    @Override
    protected double accuracy(CLLocation coords) {
        return coords.getHorizontalAccuracy();
    }

    @Override
    protected Double altitude(CLLocation coords) {
        if (coords.getVerticalAccuracy() < 0) {
            return null;
        }
        return coords.getAltitude();
    }

    @Override
    protected Double altitudeAccuracy(CLLocation coords) {
        if (coords.getVerticalAccuracy() < 0) {
            return null;
        }
        return coords.getVerticalAccuracy();
    }

    @Override
    protected Double heading(CLLocation coords) {
        if (coords.getCourse() < 0) {
            return null;
        }
        return coords.getCourse();
    }

    @Override
    protected Double speed(CLLocation coords) {
        if (coords.getSpeed() < 0) {
            return null;
        }
        return coords.getSpeed();
    }
    
    final class Adapter extends CLLocationManagerDelegateAdapter {
        final Query q;
        final CLLocationManager m;

        public Adapter(Query q, CLLocationManager m) {
            this.q = q;
            this.m = m;
        }

        private void didUpdateToLocation(CLLocationManager manager, CLLocation newLocation) {
            long time = (long)newLocation.getTimestamp().getTimeIntervalSince1970();
            RoboVMGeo.super.callback(q, time, newLocation, null);
            if (q.isOneTime()) {
                m.stopUpdatingLocation();
            }
        }

        @Override
        public void didUpdateLocations(CLLocationManager manager, NSArray<CLLocation> locations) {
            didUpdateToLocation(manager, locations.get(0));
        }

        @Override
        public void didUpdateHeading(CLLocationManager manager, CLHeading newHeading) {
        }

        @Override
        public boolean shouldDisplayHeadingCalibration(CLLocationManager manager) {
            return false;
        }

        @Override
        public void didDetermineState(CLLocationManager manager, CLRegionState state, CLRegion region) {
        }

        @Override
        public void didRangeBeacons(CLLocationManager manager, NSArray<CLBeacon> beacons, CLBeaconRegion region) {
        }

        @Override
        public void rangingBeaconsDidFail(CLLocationManager manager, CLBeaconRegion region, NSError error) {
        }

        @Override
        public void didEnterRegion(CLLocationManager manager, CLRegion region) {
        }

        @Override
        public void didExitRegion(CLLocationManager manager, CLRegion region) {
        }

        @Override
        public void didFail(CLLocationManager manager, NSError error) {
            callback(q, System.currentTimeMillis(), null, new Exception(error.getLocalizedFailureReason()));
        }

        @Override
        public void monitoringDidFail(CLLocationManager manager, CLRegion region, NSError error) {
            callback(q, System.currentTimeMillis(), null, new Exception(error.getLocalizedFailureReason()));
        }

        @Override
        public void didChangeAuthorizationStatus(CLLocationManager manager, CLAuthorizationStatus status) {
        }

        @Override
        public void didStartMonitoring(CLLocationManager manager, CLRegion region) {
        }

        @Override
        public void didPauseLocationUpdates(CLLocationManager manager) {
        }

        @Override
        public void didResumeLocationUpdates(CLLocationManager manager) {
        }

        @Override
        public void didFinishDeferredUpdates(CLLocationManager manager, NSError error) {
            callback(q, System.currentTimeMillis(), null, new Exception(error.getLocalizedFailureReason()));
        }
    }
}
