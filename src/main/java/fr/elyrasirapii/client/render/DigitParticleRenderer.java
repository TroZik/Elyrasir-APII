package fr.elyrasirapii.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class DigitParticleRenderer {
    // Représentation des chiffres 0 à 9 dans une grille 5x3
    private static final Map<Integer, int[][]> DIGITS = new HashMap<>();

    static {
        DIGITS.put(0, new int[][]{
                {1,1,1},
                {1,0,1},
                {1,0,1},
                {1,0,1},
                {1,1,1}
        });
        DIGITS.put(1, new int[][]{
                {0,1,0},
                {1,1,0},
                {0,1,0},
                {0,1,0},
                {1,1,1}
        });
        DIGITS.put(2, new int[][]{
                {1,1,1},
                {0,0,1},
                {1,1,1},
                {1,0,0},
                {1,1,1}
        });
        DIGITS.put(3, new int[][]{
                {1,1,1},
                {0,0,1},
                {0,1,1},
                {0,0,1},
                {1,1,1}
        });
        DIGITS.put(4, new int[][]{
                {1,0,1},
                {1,0,1},
                {1,1,1},
                {0,0,1},
                {0,0,1}
        });
        DIGITS.put(5, new int[][]{
                {1,1,1},
                {1,0,0},
                {1,1,1},
                {0,0,1},
                {1,1,1}
        });
        DIGITS.put(6, new int[][]{
                {1,1,1},
                {1,0,0},
                {1,1,1},
                {1,0,1},
                {1,1,1}
        });
        DIGITS.put(7, new int[][]{
                {1,1,1},
                {0,0,1},
                {0,1,0},
                {1,0,0},
                {1,0,0}
        });
        DIGITS.put(8, new int[][]{
                {1,1,1},
                {1,0,1},
                {1,1,1},
                {1,0,1},
                {1,1,1}
        });
        DIGITS.put(9, new int[][]{
                {1,1,1},
                {1,0,1},
                {1,1,1},
                {0,0,1},
                {1,1,1}
        });
    }

    private static void renderSingleDigit(Level level, double baseX, double baseY, double baseZ, float angle, int digit) {
        int[][] digitMatrix = DIGITS.getOrDefault(digit % 10, DIGITS.get(8)); // fallback = 8
        double spacing = 0.25;

        int height = digitMatrix.length;
        int width = digitMatrix[0].length;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (digitMatrix[row][col] == 1) {
                    double localX = 0;
                    double localZ = (col - (width - 1) / 2.0) * spacing;

                    double rotatedX = localX * Math.cos(angle) - localZ * Math.sin(angle);
                    double rotatedZ = localX * Math.sin(angle) + localZ * Math.cos(angle);

                    double x = baseX + rotatedX;
                    double y = baseY + (height - 1 - row) * spacing;
                    double z = baseZ + rotatedZ;

                    level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0);
                }
            }
        }
    }

    public static void renderDigit(Level level, BlockPos origin, int index) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double baseX = origin.getX() + 0.5;
        double baseY = origin.getY() + 0.5;
        double baseZ = origin.getZ() + 0.5;

        double dx = mc.player.getX() - baseX;
        double dz = mc.player.getZ() - baseZ;

        float angle = (float) Math.atan2(dz, dx) + (float) Math.PI;

        int displayNumber = index + 1;

        if (displayNumber < 10) {
            // Affichage standard pour les chiffres 1 à 9
            renderSingleDigit(level, baseX, baseY, baseZ, angle, displayNumber);
        } else {
            // Affichage pour les nombres à deux chiffres (10 à 16)
            String numStr = Integer.toString(displayNumber);
            double spacing = 0.8;

            // Centrage dynamique selon le nombre de chiffres
            double totalWidth = (numStr.length() - 1) * spacing;

            for (int i = 0; i < numStr.length(); i++) {
                int digit = Character.getNumericValue(numStr.charAt(i));

                double offset = (i - (numStr.length() - 1) / 2.0) * spacing;

                double offsetX = Math.cos(angle + Math.PI / 2) * offset;
                double offsetZ = Math.sin(angle + Math.PI / 2) * offset;

                renderSingleDigit(level, baseX + offsetX, baseY, baseZ + offsetZ, angle, digit);
            }
        }
    }


}
