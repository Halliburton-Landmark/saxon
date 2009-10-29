package net.sf.saxon.number;

import net.sf.saxon.sort.IntRangeSet;

/**
 * This class contains static utility methods to test whether a character is alphanumeric, as defined
 * by the rules of xsl:number: that is, whether it is in one of the Unicode categories
 * Nd, Nl, No, Lu, Ll, Lt, Lm or Lo
 */

public class Alphanumeric {

    private static int[] zeroDigits = {
            0x0030, 0x0660, 0x06f0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66, 0x0ce6,
            0x0d66, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x17e0, 0x1810, 0x1946, 0x19d0, 0xff10,
            0x104a0, 0x107ce, 0x107d8, 0x107e2, 0x107ec, 0x107f6 };

    // These data sets were generated from the Unicode 4.0 database using a custom stylesheet.
    // Note that the characters in the CJK Extended Ideograph ranges A and B, 3400-4DB5 and
    // 20000-2A6D6 are not listed individually\\ in the database, and therefore need to be handled specially.

    private static int[] startPoints = new int[]{
        0x0030, 0x0041, 0x0061, 0x00AA, 0x00B2, 0x00B5, 0x00B9, 0x00BC, 0x00C0, 0x00D8, 0x00F8,
        0x0250, 0x02C6, 0x02E0, 0x02EE, 0x037A, 0x0386, 0x0388, 0x038C, 0x038E, 0x03A3, 0x03D0,
        0x03F7, 0x048A, 0x04D0, 0x0500, 0x0531, 0x0559, 0x0561, 0x05D0, 0x05F0, 0x0621, 0x0640,
        0x0660, 0x066E, 0x0671, 0x06D5, 0x06E5, 0x06EE, 0x06FF, 0x0710, 0x0712, 0x074D, 0x0780,
        0x07B1, 0x0904, 0x093D, 0x0950, 0x0958, 0x0966, 0x097D, 0x0985, 0x098F, 0x0993, 0x09AA,
        0x09B2, 0x09B6, 0x09BD, 0x09CE, 0x09DC, 0x09DF, 0x09E6, 0x09F4, 0x0A05, 0x0A0F, 0x0A13,
        0x0A2A, 0x0A32, 0x0A35, 0x0A38, 0x0A59, 0x0A5E, 0x0A66, 0x0A72, 0x0A85, 0x0A8F, 0x0A93,
        0x0AAA, 0x0AB2, 0x0AB5, 0x0ABD, 0x0AD0, 0x0AE0, 0x0AE6, 0x0B05, 0x0B0F, 0x0B13, 0x0B2A,
        0x0B32, 0x0B35, 0x0B3D, 0x0B5C, 0x0B5F, 0x0B66, 0x0B71, 0x0B83, 0x0B85, 0x0B8E, 0x0B92,
        0x0B99, 0x0B9C, 0x0B9E, 0x0BA3, 0x0BA8, 0x0BAE, 0x0BE6, 0x0C05, 0x0C0E, 0x0C12, 0x0C2A,
        0x0C35, 0x0C60, 0x0C66, 0x0C85, 0x0C8E, 0x0C92, 0x0CAA, 0x0CB5, 0x0CBD, 0x0CDE, 0x0CE0,
        0x0CE6, 0x0D05, 0x0D0E, 0x0D12, 0x0D2A, 0x0D60, 0x0D66, 0x0D85, 0x0D9A, 0x0DB3, 0x0DBD,
        0x0DC0, 0x0E01, 0x0E32, 0x0E40, 0x0E50, 0x0E81, 0x0E84, 0x0E87, 0x0E8A, 0x0E8D, 0x0E94,
        0x0E99, 0x0EA1, 0x0EA5, 0x0EA7, 0x0EAA, 0x0EAD, 0x0EB2, 0x0EBD, 0x0EC0, 0x0EC6, 0x0ED0,
        0x0EDC, 0x0F00, 0x0F20, 0x0F40, 0x0F49, 0x0F88, 0x1000, 0x1023, 0x1029, 0x1040, 0x1050,
        0x10A0, 0x10D0, 0x10FC, 0x1100, 0x115F, 0x11A8, 0x1200, 0x124A, 0x1250, 0x1258, 0x125A,
        0x1260, 0x128A, 0x1290, 0x12B2, 0x12B8, 0x12C0, 0x12C2, 0x12C8, 0x12D8, 0x1312, 0x1318,
        0x1369, 0x1380, 0x13A0, 0x1401, 0x166F, 0x1681, 0x16A0, 0x16EE, 0x1700, 0x170E, 0x1720,
        0x1740, 0x1760, 0x176E, 0x1780, 0x17D7, 0x17DC, 0x17E0, 0x17F0, 0x1810, 0x1820, 0x1880,
        0x1900, 0x1946, 0x1970, 0x1980, 0x19C1, 0x19D0, 0x1A00, 0x1D00, 0x1E00, 0x1EA0, 0x1F00,
        0x1F18, 0x1F20, 0x1F48, 0x1F50, 0x1F59, 0x1F5B, 0x1F5D, 0x1F5F, 0x1F80, 0x1FB6, 0x1FBE,
        0x1FC2, 0x1FC6, 0x1FD0, 0x1FD6, 0x1FE0, 0x1FF2, 0x1FF6, 0x2070, 0x2074, 0x207F, 0x2090,
        0x2102, 0x2107, 0x210A, 0x2115, 0x2119, 0x2124, 0x2126, 0x2128, 0x212A, 0x212F, 0x2133,
        0x213C, 0x2145, 0x2153, 0x2460, 0x24EA, 0x2776, 0x2C00, 0x2C30, 0x2C80, 0x2CFD, 0x2D00,
        0x2D30, 0x2D6F, 0x2D80, 0x2DA0, 0x2DA8, 0x2DB0, 0x2DB8, 0x2DC0, 0x2DC8, 0x2DD0, 0x2DD8,
        0x3005, 0x3021, 0x3031, 0x3038, 0x3041, 0x309D, 0x30A1, 0x30FC, 0x3105, 0x3131, 0x3192,
        0x31A0, 0x31F0, 0x3220, 0x3251, 0x3280, 0x32B1, 0x3400, 0x4E00, /*0x9FBB,*/ 0xA000,
        0xA800, 0xA803, 0xA807, 0xA80C, 0xAC00, /*0xD7A3,*/ 0xF900, 0xFA30, 0xFA70, 0xFB00, 0xFB13,
        0xFB1D, 0xFB1F, 0xFB2A, 0xFB38, 0xFB3E, 0xFB40, 0xFB43, 0xFB46, 0xFBD3, 0xFD50, 0xFD92,
        0xFDF0, 0xFE70, 0xFE76, 0xFF10, 0xFF21, 0xFF41, 0xFF66, 0xFFC2, 0xFFCA, 0xFFD2, 0xFFDA,
        0x10000, 0x1000D, 0x10028, 0x1003C, 0x1003F, 0x10050, 0x10080, 0x10107, 0x10140, 0x1018A,
        0x10300, 0x10320, 0x10330, 0x10380, 0x103A0, 0x103C8, 0x103D1, 0x10400, 0x104A0, 0x10800,
        0x10808, 0x1080A, 0x10837, 0x1083C, 0x1083F, 0x10A00, 0x10A10, 0x10A15, 0x10A19, 0x10A40,
        0x1D400, 0x1D456, 0x1D49E, 0x1D4A2, 0x1D4A5, 0x1D4A9, 0x1D4AE, 0x1D4BB, 0x1D4BD, 0x1D4C5,
        0x1D507, 0x1D50D, 0x1D516, 0x1D51E, 0x1D53B, 0x1D540, 0x1D546, 0x1D54A, 0x1D552, 0x1D6A8,
        0x1D6C2, 0x1D6DC, 0x1D6FC, 0x1D716, 0x1D736, 0x1D750, 0x1D770, 0x1D78A, 0x1D7AA, 0x1D7C4,
        0x1D7CE, 0x20000, 0x2F800 };

    private static int[] endPoints = new int[]{
        0x0039, 0x005A, 0x007A, 0x00AA, 0x00B3, 0x00B5, 0x00BA, 0x00BE, 0x00D6, 0x00F6, 0x0241,
        0x02C1, 0x02D1, 0x02E4, 0x02EE, 0x037A, 0x0386, 0x038A, 0x038C, 0x03A1, 0x03CE, 0x03F5,
        0x0481, 0x04CE, 0x04F9, 0x050F, 0x0556, 0x0559, 0x0587, 0x05EA, 0x05F2, 0x063A, 0x064A,
        0x0669, 0x066F, 0x06D3, 0x06D5, 0x06E6, 0x06FC, 0x06FF, 0x0710, 0x072F, 0x076D, 0x07A5,
        0x07B1, 0x0939, 0x093D, 0x0950, 0x0961, 0x096F, 0x097D, 0x098C, 0x0990, 0x09A8, 0x09B0,
        0x09B2, 0x09B9, 0x09BD, 0x09CE, 0x09DD, 0x09E1, 0x09F1, 0x09F9, 0x0A0A, 0x0A10, 0x0A28,
        0x0A30, 0x0A33, 0x0A36, 0x0A39, 0x0A5C, 0x0A5E, 0x0A6F, 0x0A74, 0x0A8D, 0x0A91, 0x0AA8,
        0x0AB0, 0x0AB3, 0x0AB9, 0x0ABD, 0x0AD0, 0x0AE1, 0x0AEF, 0x0B0C, 0x0B10, 0x0B28, 0x0B30,
        0x0B33, 0x0B39, 0x0B3D, 0x0B5D, 0x0B61, 0x0B6F, 0x0B71, 0x0B83, 0x0B8A, 0x0B90, 0x0B95,
        0x0B9A, 0x0B9C, 0x0B9F, 0x0BA4, 0x0BAA, 0x0BB9, 0x0BF2, 0x0C0C, 0x0C10, 0x0C28, 0x0C33,
        0x0C39, 0x0C61, 0x0C6F, 0x0C8C, 0x0C90, 0x0CA8, 0x0CB3, 0x0CB9, 0x0CBD, 0x0CDE, 0x0CE1,
        0x0CEF, 0x0D0C, 0x0D10, 0x0D28, 0x0D39, 0x0D61, 0x0D6F, 0x0D96, 0x0DB1, 0x0DBB, 0x0DBD,
        0x0DC6, 0x0E30, 0x0E33, 0x0E46, 0x0E59, 0x0E82, 0x0E84, 0x0E88, 0x0E8A, 0x0E8D, 0x0E97,
        0x0E9F, 0x0EA3, 0x0EA5, 0x0EA7, 0x0EAB, 0x0EB0, 0x0EB3, 0x0EBD, 0x0EC4, 0x0EC6, 0x0ED9,
        0x0EDD, 0x0F00, 0x0F33, 0x0F47, 0x0F6A, 0x0F8B, 0x1021, 0x1027, 0x102A, 0x1049, 0x1055,
        0x10C5, 0x10FA, 0x10FC, 0x1159, 0x11A2, 0x11F9, 0x1248, 0x124D, 0x1256, 0x1258, 0x125D,
        0x1288, 0x128D, 0x12B0, 0x12B5, 0x12BE, 0x12C0, 0x12C5, 0x12D6, 0x1310, 0x1315, 0x135A,
        0x137C, 0x138F, 0x13F4, 0x166C, 0x1676, 0x169A, 0x16EA, 0x16F0, 0x170C, 0x1711, 0x1731,
        0x1751, 0x176C, 0x1770, 0x17B3, 0x17D7, 0x17DC, 0x17E9, 0x17F9, 0x1819, 0x1877, 0x18A8,
        0x191C, 0x196D, 0x1974, 0x19A9, 0x19C7, 0x19D9, 0x1A16, 0x1DBF, 0x1E9B, 0x1EF9, 0x1F15,
        0x1F1D, 0x1F45, 0x1F4D, 0x1F57, 0x1F59, 0x1F5B, 0x1F5D, 0x1F7D, 0x1FB4, 0x1FBC, 0x1FBE,
        0x1FC4, 0x1FCC, 0x1FD3, 0x1FDB, 0x1FEC, 0x1FF4, 0x1FFC, 0x2071, 0x2079, 0x2089, 0x2094,
        0x2102, 0x2107, 0x2113, 0x2115, 0x211D, 0x2124, 0x2126, 0x2128, 0x212D, 0x2131, 0x2139,
        0x213F, 0x2149, 0x2183, 0x249B, 0x24FF, 0x2793, 0x2C2E, 0x2C5E, 0x2CE4, 0x2CFD, 0x2D25,
        0x2D65, 0x2D6F, 0x2D96, 0x2DA6, 0x2DAE, 0x2DB6, 0x2DBE, 0x2DC6, 0x2DCE, 0x2DD6, 0x2DDE,
        0x3007, 0x3029, 0x3035, 0x303C, 0x3096, 0x309F, 0x30FA, 0x30FF, 0x312C, 0x318E, 0x3195,
        0x31B7, 0x31FF, 0x3229, 0x325F, 0x3289, 0x32BF, 0x4DB5, /*0x4E00,*/ 0x9FBB, 0xA48C,
        0xA801, 0xA805, 0xA80A, 0xA822, /*0xAC00,*/ 0xD7A3, 0xFA2D, 0xFA6A, 0xFAD9, 0xFB06, 0xFB17,
        0xFB1D, 0xFB28, 0xFB36, 0xFB3C, 0xFB3E, 0xFB41, 0xFB44, 0xFBB1, 0xFD3D, 0xFD8F, 0xFDC7,
        0xFDFB, 0xFE74, 0xFEFC, 0xFF19, 0xFF3A, 0xFF5A, 0xFFBE, 0xFFC7, 0xFFCF, 0xFFD7, 0xFFDC,
        0x1000B, 0x10026, 0x1003A, 0x1003D, 0x1004D, 0x1005D, 0x100FA, 0x10133, 0x10178, 0x1018A,
        0x1031E, 0x10323, 0x1034A, 0x1039D, 0x103C3, 0x103CF, 0x103D5, 0x1049D, 0x104A9, 0x10805,
        0x10808, 0x10835, 0x10838, 0x1083C, 0x1083F, 0x10A00, 0x10A13, 0x10A17, 0x10A33, 0x10A47,
        0x1D454, 0x1D49C, 0x1D49F, 0x1D4A2, 0x1D4A6, 0x1D4AC, 0x1D4B9, 0x1D4BB, 0x1D4C3, 0x1D505,
        0x1D50A, 0x1D514, 0x1D51C, 0x1D539, 0x1D53E, 0x1D544, 0x1D546, 0x1D550, 0x1D6A5, 0x1D6C0,
        0x1D6DA, 0x1D6FA, 0x1D714, 0x1D734, 0x1D74E, 0x1D76E, 0x1D788, 0x1D7A8, 0x1D7C2, 0x1D7C9,
        0x1D7FF, 0x2A6D6, 0x2FA1D };

    private static IntRangeSet alphanumerics = new IntRangeSet(startPoints, endPoints);

    /**
     * Determine whether a Unicode codepoint is alphanumeric, that is, whether it is in one of the
     * categories Nd, Nl, No, Lu, Ll, Lt, Lm or Lo
     * @param codepoint the codepoint to be tested
     * @return true if the codepoint is in one of these categories
     */

    public static boolean isAlphanumeric(int codepoint) {
        return alphanumerics.contains(codepoint);
    }

    /**
     * Determine whether a character represents a decimal digit and if so, which digit.
     * @param in the Unicode character being tested.
     * @return -1 if it's not a decimal digit, otherwise the digit value.
     */

    public static int getDigitValue(int in) {
        for (int z=0; z<zeroDigits.length; z++) {
            if (in <= zeroDigits[z]+9) {
                if (in >= zeroDigits[z]) {
                    return in - zeroDigits[z];
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }

    private Alphanumeric(){}
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

