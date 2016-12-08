package hcil.umd.edu.cmsc434motionsensorart;

import android.graphics.Color;

import java.util.Random;

/**
 * Created by jonf on 12/8/2016.
 * Preferably, would just call this ColorUtils but that's already taken in the Android library
 */

public final class ArtColorUtils {

    private static Random _random = new Random();

    // should not ever be constructed, purely a utility class
    private ArtColorUtils(){}

    public static int getRandomOpaqueColor(){
        int r = _random.nextInt(255);
        int g = _random.nextInt(255);
        int b = _random.nextInt(255);
        //int b = 50 + (int)(_random.nextFloat() * (255-50));
        return Color.argb(255, r, g, b);
    }
}
