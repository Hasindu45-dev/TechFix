package com.techfix.app.utils;

import android.location.Location;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.models.Branch;
import com.techfix.app.models.SparePart;
import com.techfix.app.models.Technician;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BranchAssignmentUtility {

    public interface OnAssignmentCompleteListener {
        void onAssignmentComplete(Branch assignedBranch, double distanceKm, String assignedTechnicianName, String reason);
        void onAssignmentFailed(String errorMsg);
    }

    public static void assignBranch(
            double customerLat,
            double customerLng,
            String category, // "Computer" or "Mobile"
            String serviceName,
            OnAssignmentCompleteListener listener) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Determine required spare part
        String requiredPart = getRequiredPartForService(serviceName);

        // Seed data verification first (ensures database is not empty)
        seedTechniciansAndPartsIfEmpty(db, () -> {
            
            // 2. Fetch all Branches
            db.collection("branches").get().addOnCompleteListener(branchTask -> {
                if (!branchTask.isSuccessful() || branchTask.getResult() == null) {
                    listener.onAssignmentFailed("Failed to fetch branches");
                    return;
                }
                
                List<Branch> branches = new ArrayList<>();
                for (DocumentSnapshot doc : branchTask.getResult()) {
                    Branch b = doc.toObject(Branch.class);
                    if (b != null) branches.add(b);
                }

                if (branches.isEmpty()) {
                    listener.onAssignmentFailed("No branches found in system");
                    return;
                }

                // 3. Fetch Technicians
                db.collection("technicians").get().addOnCompleteListener(techTask -> {
                    List<Technician> technicians = new ArrayList<>();
                    if (techTask.isSuccessful() && techTask.getResult() != null) {
                        for (DocumentSnapshot doc : techTask.getResult()) {
                            Technician t = doc.toObject(Technician.class);
                            if (t != null) technicians.add(t);
                        }
                    }

                    // 4. Fetch Spare Parts
                    db.collection("spareParts").get().addOnCompleteListener(partTask -> {
                        List<SparePart> parts = new ArrayList<>();
                        if (partTask.isSuccessful() && partTask.getResult() != null) {
                            for (DocumentSnapshot doc : partTask.getResult()) {
                                SparePart p = doc.toObject(SparePart.class);
                                if (p != null) parts.add(p);
                            }
                        }

                        // 5. Run Assignment Algorithm
                        processAssignment(customerLat, customerLng, category, requiredPart, 
                                          branches, technicians, parts, listener);
                    });
                });
            });
        });
    }

    private static void processAssignment(
            double customerLat,
            double customerLng,
            String category,
            String requiredPart,
            List<Branch> branches,
            List<Technician> technicians,
            List<SparePart> parts,
            OnAssignmentCompleteListener listener) {

        Branch bestBranch = null;
        double minDistance = Double.MAX_VALUE;
        String assignedTechName = "Unassigned";
        String assignmentReason = "";

        // Sort branches by distance to customer first
        List<BranchWithDistance> branchDistances = new ArrayList<>();
        for (Branch b : branches) {
            float[] result = new float[1];
            Location.distanceBetween(customerLat, customerLng, b.getLatitude(), b.getLongitude(), result);
            double distanceKm = result[0] / 1000.0;
            branchDistances.add(new BranchWithDistance(b, distanceKm));
        }
        Collections.sort(branchDistances, (o1, o2) -> Double.compare(o1.distance, o2.distance));

        // Iterate through closest branches to find one that satisfies ALL requirements:
        // Requirement 1: Available Technician for the device type
        // Requirement 2: Required spare part in stock (quantity > 0)
        for (BranchWithDistance bd : branchDistances) {
            Branch b = bd.branch;
            double distance = bd.distance;

            // Check technician availability
            Technician availableTech = findAvailableTechnician(b.getBranchId(), category, technicians);
            
            // Check spare part availability
            boolean partAvailable = requiredPart == null || isPartAvailable(b.getBranchId(), requiredPart, parts);

            if (availableTech != null && partAvailable) {
                bestBranch = b;
                minDistance = distance;
                assignedTechName = availableTech.getName();
                assignmentReason = "Closest branch with available " + category + " technician and required parts in stock.";
                break; // Found the closest branch matching all criteria!
            }
        }

        // Fallback 1: If no branch matches all criteria, look for closest branch with technician (even if out of stock)
        if (bestBranch == null) {
            for (BranchWithDistance bd : branchDistances) {
                Branch b = bd.branch;
                double distance = bd.distance;
                Technician availableTech = findAvailableTechnician(b.getBranchId(), category, technicians);

                if (availableTech != null) {
                    bestBranch = b;
                    minDistance = distance;
                    assignedTechName = availableTech.getName();
                    assignmentReason = "Closest branch with available technician (Note: Parts out of stock; pending order).";
                    break;
                }
            }
        }

        // Fallback 2: Absolute fallback to closest physical branch overall
        if (bestBranch == null && !branchDistances.isEmpty()) {
            BranchWithDistance closest = branchDistances.get(0);
            bestBranch = closest.branch;
            minDistance = closest.distance;
            assignedTechName = "Waiting Allocation";
            assignmentReason = "Closest branch overall (Technician currently busy, queue allocation pending).";
        }

        if (bestBranch != null) {
            listener.onAssignmentComplete(bestBranch, minDistance, assignedTechName, assignmentReason);
        } else {
            listener.onAssignmentFailed("Could not assign any branch.");
        }
    }

    private static Technician findAvailableTechnician(String branchId, String category, List<Technician> techs) {
        // Map category ("Computer", "Mobile") to specialization ("Laptop", "Mobile")
        String specialization = "Computer".equalsIgnoreCase(category) ? "Laptop" : "Mobile";
        for (Technician t : techs) {
            if (t.getBranchId().equalsIgnoreCase(branchId) 
                    && t.getSpecialization().equalsIgnoreCase(specialization) 
                    && t.isAvailability()) {
                return t;
            }
        }
        return null;
    }

    private static boolean isPartAvailable(String branchId, String partName, List<SparePart> parts) {
        for (SparePart p : parts) {
            if (p.getBranchId().equalsIgnoreCase(branchId) 
                    && p.getName().toLowerCase().contains(partName.toLowerCase()) 
                    && p.getQuantity() > 0) {
                return true;
            }
        }
        return false;
    }

    private static String getRequiredPartForService(String serviceName) {
        if (serviceName == null) return null;
        String sLower = serviceName.toLowerCase();
        
        if (sLower.contains("screen") && sLower.contains("laptop")) {
            return "Laptop Screen";
        } else if (sLower.contains("screen") && sLower.contains("mobile")) {
            return "Mobile Screen";
        } else if (sLower.contains("battery")) {
            return "Mobile Battery";
        } else if (sLower.contains("port")) {
            return "Charging Port";
        } else if (sLower.contains("ram") || sLower.contains("ssd") || sLower.contains("upgrade")) {
            return "SSD";
        }
        return null; // Software/OS services do not need hardware parts
    }

    private static void seedTechniciansAndPartsIfEmpty(FirebaseFirestore db, Runnable onComplete) {
        // Check if technicians exist
        db.collection("technicians").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().isEmpty()) {
                
                // Seed Technicians
                List<Technician> techs = new ArrayList<>();
                techs.add(new Technician("t1", "Amal Silva", "Laptop", "colombo", true));
                techs.add(new Technician("t2", "Nimal Perera", "Mobile", "colombo", true));
                techs.add(new Technician("t3", "Sunil Fernando", "Laptop", "galle", false)); // Out of office
                techs.add(new Technician("t4", "Kasun Jayasundara", "Mobile", "galle", true));

                for (Technician t : techs) {
                    db.collection("technicians").document(t.getTechnicianId()).set(t);
                }

                // Seed Spare Parts
                List<SparePart> parts = new ArrayList<>();
                parts.add(new SparePart("p1", "Laptop Screen", 5, 12000.0, "colombo"));
                parts.add(new SparePart("p2", "Mobile Screen", 10, 8000.0, "colombo"));
                parts.add(new SparePart("p3", "Mobile Battery", 0, 3500.0, "colombo")); // Out of stock at Colombo!
                
                parts.add(new SparePart("p4", "Laptop Screen", 0, 12000.0, "galle")); // Out of stock at Galle!
                parts.add(new SparePart("p5", "Mobile Screen", 8, 8000.0, "galle"));
                parts.add(new SparePart("p6", "Mobile Battery", 15, 3500.0, "galle")); // In stock at Galle!
                parts.add(new SparePart("p7", "SSD", 12, 9500.0, "colombo"));
                parts.add(new SparePart("p8", "SSD", 5, 9500.0, "galle"));

                for (SparePart p : parts) {
                    db.collection("spareParts").document(p.getPartId()).set(p);
                }
            }
            onComplete.run();
        });
    }

    private static class BranchWithDistance {
        Branch branch;
        double distance;

        BranchWithDistance(Branch branch, double distance) {
            this.branch = branch;
            this.distance = distance;
        }
    }
}
