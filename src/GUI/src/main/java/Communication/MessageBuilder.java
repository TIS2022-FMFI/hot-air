package Communication;

import java.util.Arrays;

public class MessageBuilder {

    public static class EXE {
        private static final byte ID = 'a';

        public static byte[] build() {return new byte[] {ID};}
        public static boolean is(byte msg) {return msg == ID;}

        public static class FileTransfer {
            private static final byte ID = 'a';

            public static byte[] build() {
                return new byte[]{EXE.ID, FileTransfer.ID};
            }
            public static boolean equals(byte[] msg) {
                return Arrays.equals(msg, new byte[]{EXE.ID, FileTransfer.ID});
            }
        }

        public static class EndOfSegment {
            private static final byte ID = 'b';

            public static byte[] build() {
                return new byte[]{EXE.ID, ID};
            }
            public static boolean equals(byte[] msg) {
                return Arrays.equals(msg, new byte[]{EXE.ID, ID});
            }
        }
    }

    public static class GUI {
        private static final byte ID = 'b';
        public static byte[] build() {
            return new byte[]{ID};
        }
        public static boolean is(byte msg) {return msg == ID;}

        public static class Request {
            private static final byte ID = 'a';

            public static class NumberOfControllers {
                private static final byte ID = 'a';

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[] {GUI.ID, Request.ID, ID});}
            }

            public static class NumberOfProjects {
                private static final byte ID = 'b';

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[] {GUI.ID, Request.ID, ID});}
            }

            public static class ChangeControllerID {
                private static final byte ID = 'c';

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[] {GUI.ID, Request.ID, ID});}
            }

            public static class SearchForNewControllers {
                private static final byte ID = 'd';

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class BigRedButton {
                private static final byte ID = 'e';

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class StopThisController {
                private static final byte ID = 'e'+1; //alphabet is hard, ok?

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class GetInfoAboutControllers {
                private static final byte ID = 'e'+2;

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class GetInfoAboutProjects {
                private static final byte ID = 'e'+3;

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class TemperatureChanged {
                private static final byte ID = 'e'+4;

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class UnlockThisController {
                private static final byte ID = 'e'+5;

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class RequestTemperatureLog {
                private static final byte ID = 'e'+6;

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }

            public static class RequestCheckForOldLogFiles {
                private static final byte ID = 'e'+7;

                public static byte[] build() {return new byte[]{GUI.ID, Request.ID, ID};}
                public static boolean equals(byte[] msg) {return Arrays.equals(msg, new byte[]{GUI.ID, Request.ID, ID});}
            }
        }
        public static class Exception {
            private static final byte ID = 'b';

            public static byte[] build() {
                return new byte[]{GUI.ID, ID};
            }
            public static boolean equals(byte[] msg) {
                return Arrays.equals(msg, new byte[]{GUI.ID, ID});
            }
        }

    }

    public static class Controller {
        private static final byte ID = (byte) 128;

        public static byte[] build() {
            return new byte[]{ID};
        }
        public static boolean equals(byte[] msg) {
            return Arrays.equals(msg, new byte[]{ID});
        }
        public static boolean is(byte msg) {return msg == ID;}
    }
}