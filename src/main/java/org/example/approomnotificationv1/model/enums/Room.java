package org.example.roomnotificationtest_1.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Room {
    SPOTIFY(314),
    APPLE(201),
    YOUTUBE(202),
    ALIBABA(203),
    CISCO(204),
    MICROSOFT(205),
    ZOOM(206),
    NETFLIX(207),
    AMAZON(208),
    GOOGLE(209),
    INSTAGRAM(210),
    FACEBOOK(301),
    TELEGRAM(302);
//    private String emoji;
    private final int num;

    @Override
    public String toString() {
        return this.name() + " " + num;
    }
}
