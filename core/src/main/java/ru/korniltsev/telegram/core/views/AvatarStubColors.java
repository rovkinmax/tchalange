package ru.korniltsev.telegram.core.views;

public class AvatarStubColors {
    private static int[] arrColorsProfiles = {0xffd86f65, 0xfff69d61, 0xfffabb3c, 0xff67b35d, 0xff56a2bb, 0xff5c98cd, 0xff8c79d2, 0xfff37fa6};
    public static int getColorFor(int id) {
        return arrColorsProfiles[Math.abs(id) % arrColorsProfiles.length];
    }
}
