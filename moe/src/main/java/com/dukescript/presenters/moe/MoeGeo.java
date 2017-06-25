
package com.dukescript.presenters.moe;

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

import org.netbeans.html.geo.spi.GLProvider;
import org.openide.util.lookup.ServiceProvider;
import apple.corelocation.enums.CLAuthorizationStatus;
import apple.corelocation.CLLocation;
import apple.corelocation.CLLocationManager;
import apple.corelocation.protocol.CLLocationManagerDelegate;
import apple.corelocation.CLRegion;
import apple.foundation.NSArray;
import apple.foundation.NSError;

/** Implements geolocation services for Multi OS Engine.
 * Use {@link OnLocation} annotation to access this implementation.
 */
@ServiceProvider(service = GLProvider.class)
public final class MoeGeo extends GLProvider<CLLocation,MoeGeo.Adapter> {
    @Override
    protected Adapter start(Query query) {
        if (
            CLLocationManager.authorizationStatus() == CLAuthorizationStatus.Denied
        ) {
            return null;
        }
        CLLocationManager cl = CLLocationManager.alloc();
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
        return coords.coordinate().latitude();
    }

    @Override
    protected double longitude(CLLocation coords) {
        return coords.coordinate().longitude();
    }

    @Override
    protected double accuracy(CLLocation coords) {
        return coords.horizontalAccuracy();
    }

    @Override
    protected Double altitude(CLLocation coords) {
        if (coords.verticalAccuracy() < 0) {
            return null;
        }
        return coords.altitude();
    }

    @Override
    protected Double altitudeAccuracy(CLLocation coords) {
        if (coords.verticalAccuracy() < 0) {
            return null;
        }
        return coords.verticalAccuracy();
    }

    @Override
    protected Double heading(CLLocation coords) {
        if (coords.course() < 0) {
            return null;
        }
        return coords.course();
    }

    @Override
    protected Double speed(CLLocation coords) {
        if (coords.speed() < 0) {
            return null;
        }
        return coords.speed();
    }
    
    final class Adapter implements CLLocationManagerDelegate {
        final Query q;
        final CLLocationManager m;

        public Adapter(Query q, CLLocationManager m) {
            this.q = q;
            this.m = m;
        }

        @Override
        public void locationManagerDidUpdateLocations(CLLocationManager manager, NSArray<? extends CLLocation> locations) {
            didUpdateToLocation(manager, locations.get(0), null);
        }



        public void didUpdateToLocation(CLLocationManager manager, CLLocation newLocation, CLLocation oldLocation) {
            long time = (long)newLocation.timestamp().timeIntervalSince1970();
            MoeGeo.super.callback(q, time, newLocation, null);
            if (q.isOneTime()) {
                m.stopUpdatingLocation();
            }
        }

        @Override
        public boolean locationManagerShouldDisplayHeadingCalibration(CLLocationManager manager) {
            return false;
        }

        @Override
        public void locationManagerDidFailWithError(CLLocationManager manager, NSError error) {
            callback(q, System.currentTimeMillis(), null, new Exception(error.localizedFailureReason()));
        }

        @Override
        public void locationManagerMonitoringDidFailForRegionWithError(CLLocationManager manager, CLRegion region, NSError error) {
            callback(q, System.currentTimeMillis(), null, new Exception(error.localizedFailureReason()));
        }

        @Override
        public void locationManagerDidFinishDeferredUpdatesWithError(CLLocationManager manager, NSError error) {
            callback(q, System.currentTimeMillis(), null, new Exception(error.localizedFailureReason()));
        }
    }
}
