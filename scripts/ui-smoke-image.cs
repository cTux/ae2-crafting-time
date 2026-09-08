using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;

public static class SmokePixels {
    public static Bitmap Read(string path) {
        using (var input = File.OpenRead(path)) {
            if (input.Length < 24 || input.Length > 67108864) throw new InvalidDataException("PNG size outside bounds");
            var header = new byte[24];
            if (input.Read(header, 0, 24) != 24) throw new InvalidDataException("Truncated PNG");
            byte[] signature = {137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82};
            for (int i = 0; i < signature.Length; i++)
                if (header[i] != signature[i]) throw new InvalidDataException("Invalid PNG header");
            long width = 0, height = 0;
            for (int i = 0; i < 4; i++) { width = width * 256 + header[16+i]; height = height * 256 + header[20+i]; }
            if (width < 1 || height < 1 || width > 16384 || height > 16384 || width * height > 67108864)
                throw new InvalidDataException("PNG dimensions outside bounds");
            input.Position = 0;
            using (var decoded = Image.FromStream(input, true, true)) {
                if (decoded.Width != width || decoded.Height != height) throw new InvalidDataException("PNG dimension mismatch");
                return new Bitmap(decoded);
            }
        }
    }

    public static void Bounds(int[] rect, int width, int height) {
        if (rect == null || rect.Length != 4 || rect[0] < 0 || rect[1] < 0 || rect[2] <= 0 || rect[3] <= 0 ||
            (long)rect[0]+rect[2] > width || (long)rect[1]+rect[3] > height)
            throw new InvalidDataException("Crop or mask outside bounds");
    }

    public static long Compare(Bitmap actual, Bitmap expected, int[] crop, int[][] masks, string diffPath) {
        Bounds(crop, actual.Width, actual.Height);
        if (expected.Width != crop[2] || expected.Height != crop[3]) throw new InvalidDataException("Baseline dimensions do not match crop");
        foreach (var mask in masks) Bounds(mask, crop[2], crop[3]);
        long changed = 0, compared = 0;
        using (var diff = new Bitmap(crop[2], crop[3])) {
            for (int y = 0; y < crop[3]; y++) for (int x = 0; x < crop[2]; x++) {
                bool masked = false;
                foreach (var mask in masks) if (x >= mask[0] && y >= mask[1] && x < mask[0]+mask[2] && y < mask[1]+mask[3]) masked = true;
                if (masked) continue;
                compared++;
                var a = actual.GetPixel(crop[0]+x, crop[1]+y);
                var b = expected.GetPixel(x, y);
                if (a.R != b.R || a.G != b.G || a.B != b.B) { changed++; diff.SetPixel(x,y,Color.Magenta); }
                else diff.SetPixel(x,y,Color.FromArgb(a.R/3,a.G/3,a.B/3));
            }
            if (compared == 0) throw new InvalidDataException("Masks cover the entire crop");
            if (changed > 0) {
                diff.Save(diffPath, ImageFormat.Png);
                using (var region = actual.Clone(new Rectangle(crop[0],crop[1],crop[2],crop[3]), PixelFormat.Format32bppArgb))
                    region.Save(Path.ChangeExtension(diffPath,"actual.png"), ImageFormat.Png);
                expected.Save(Path.ChangeExtension(diffPath,"expected.png"), ImageFormat.Png);
            }
        }
        return changed;
    }
}
