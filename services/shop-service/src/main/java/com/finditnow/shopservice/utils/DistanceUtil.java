package com.finditnow.shopservice.utils;

import com.finditnow.shopservice.dto.Location;

public class DistanceUtil {

    private static final double EARTH_RADIUS_KM = 6371;

    public static double km(Location from, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - from.lat());
        double dLng = Math.toRadians(lng2 - from.lng());

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(from.lat())) *
                                Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}