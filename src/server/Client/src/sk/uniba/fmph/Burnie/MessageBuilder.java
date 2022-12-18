package sk.uniba.fmph.Burnie;

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
        private static final byte ID = 'c';

        public static byte[] build() {
            return new byte[]{ID};
        }
        public static boolean equals(byte[] msg) {
            return Arrays.equals(msg, new byte[]{ID});
        }
        public static boolean is(byte msg) {return msg == ID;}
    }
}