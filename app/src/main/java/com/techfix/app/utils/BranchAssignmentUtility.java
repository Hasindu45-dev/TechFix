package com.techfix.app.utils;

import android.location.Location;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.models.Branch;
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.Service;
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

        // Seed data verification first (ensures database is not empty)
        seedTechniciansAndPartsIfEmpty(db, () -> {
            
            // 1. Fetch Service to determine required parts
            db.collection("services").whereEqualTo("name", serviceName).get()
                .addOnCompleteListener(serviceTask -> {
                    final List<RequiredPart> requiredParts = new ArrayList<>();
                    if (serviceTask.isSuccessful() && serviceTask.getResult() != null && !serviceTask.getResult().isEmpty()) {
                        DocumentSnapshot doc = serviceTask.getResult().getDocuments().get(0);
                        Service s = doc.toObject(Service.class);
                        if (s != null && s.getRequiredParts() != null) {
                            requiredParts.addAll(s.getRequiredParts());
                        }
                    } else {
                        // Fallback to legacy string parsing
                        String legacyPart = getRequiredPartForService(serviceName);
                        if (legacyPart != null) {
                            requiredParts.add(new RequiredPart(legacyPart, 1));
                        }
                    }

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
                                processAssignment(customerLat, customerLng, category, requiredParts, 
                                                  branches, technicians, parts, listener);
                            });
                        });
                    });
                });
        });
    }

    private static void processAssignment(
            double customerLat,
            double customerLng,
            String category,
            List<RequiredPart> requiredParts,
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
        // Requirement 2: Required spare part in stock
        for (BranchWithDistance bd : branchDistances) {
            Branch b = bd.branch;
            double distance = bd.distance;

            // Check technician availability
            Technician availableTech = findAvailableTechnician(b.getBranchId(), category, technicians);
            
            // Check spare parts availability
            boolean partsAvailable = true;
            if (requiredParts != null) {
                for (RequiredPart req : requiredParts) {
                    if (!isPartAvailable(b.getBranchId(), req.getPartName(), req.getQuantity(), parts)) {
                        partsAvailable = false;
                        break;
                    }
                }
            }

            if (availableTech != null && partsAvailable) {
                bestBranch = b;
                minDistance = distance;
                assignedTechName = availableTech.getName();
                assignmentReason = "Closest branch with available " + category + " technician and required parts in stock.";
                break; // Found the closest branch matching all criteria!
            }
        }

        // Return results: if no branch matches all criteria, we signal with null branch to represent "Waiting for Spare Parts" status
        if (bestBranch == null) {
            listener.onAssignmentComplete(null, 0.0, "Unassigned", "No suitable branch with available parts and technicians.");
        } else {
            listener.onAssignmentComplete(bestBranch, minDistance, assignedTechName, assignmentReason);
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

    private static boolean isPartAvailable(String branchId, String partName, int requiredQty, List<SparePart> parts) {
        for (SparePart p : parts) {
            if (p.getBranchId().equalsIgnoreCase(branchId) 
                    && p.getName().toLowerCase().contains(partName.toLowerCase()) 
                    && p.getQuantity() >= requiredQty) {
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
        } else if (sLower.contains("battery") && sLower.contains("laptop")) {
            return "Laptop Battery";
        } else if (sLower.contains("battery") && sLower.contains("mobile")) {
            return "Mobile Battery";
        } else if (sLower.contains("battery")) {
            return "Mobile Battery";
        } else if (sLower.contains("keyboard")) {
            return "Laptop Keyboard";
        } else if (sLower.contains("motherboard") || sLower.contains("chip")) {
            return "Motherboard IC Chip";
        } else if (sLower.contains("camera")) {
            return "Camera Module";
        } else if (sLower.contains("speaker") || sLower.contains("mic")) {
            return "Speaker Module";
        } else if (sLower.contains("port")) {
            return "USB-C Charging Port";
        } else if (sLower.contains("ram") || sLower.contains("ssd") || sLower.contains("upgrade")) {
            return "SSD";
        } else if (sLower.contains("wifi") || sLower.contains("network")) {
            return "Wi-Fi Antenna Module";
        }
        return null; // Software/OS/Clean services do not need hardware parts
    }

    public static void seedTechniciansAndPartsIfEmpty(FirebaseFirestore db, Runnable onComplete) {
        // 1. Seed Technicians if collection is empty
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
            }
            
            // 2. Seed Spare Parts if collection is empty or missing the new modules (less than 10 documents)
            db.collection("spareParts").get().addOnCompleteListener(partTask -> {
                if (partTask.isSuccessful() && partTask.getResult() != null) {
                    if (partTask.getResult().isEmpty() || partTask.getResult().size() < 10) {
                        List<SparePart> parts = new ArrayList<>();
                        // Screens
                        parts.add(new SparePart("p1", "Laptop Screen", 5, 12000.0, "colombo"));
                        parts.add(new SparePart("p2", "Mobile Screen", 10, 8000.0, "colombo"));
                        parts.add(new SparePart("p3", "Mobile Battery", 0, 3500.0, "colombo")); // Out of stock at Colombo!
                        
                        parts.add(new SparePart("p4", "Laptop Screen", 0, 12000.0, "galle")); // Out of stock at Galle!
                        parts.add(new SparePart("p5", "Mobile Screen", 8, 8000.0, "galle"));
                        parts.add(new SparePart("p6", "Mobile Battery", 15, 3500.0, "galle")); // In stock at Galle!
                        
                        // SSD
                        parts.add(new SparePart("p7", "SSD", 12, 9500.0, "colombo"));
                        parts.add(new SparePart("p8", "SSD", 5, 9500.0, "galle"));

                        // Keyboard
                        parts.add(new SparePart("p9", "Laptop Keyboard", 4, 4500.0, "colombo"));
                        parts.add(new SparePart("p10", "Laptop Keyboard", 2, 4500.0, "galle"));

                        // Motherboard IC
                        parts.add(new SparePart("p11", "Motherboard IC Chip", 3, 2500.0, "colombo"));
                        parts.add(new SparePart("p12", "Motherboard IC Chip", 1, 2500.0, "galle"));

                        // Charging Port
                        parts.add(new SparePart("p13", "USB-C Charging Port", 15, 800.0, "colombo"));
                        parts.add(new SparePart("p14", "USB-C Charging Port", 10, 800.0, "galle"));

                        // Camera Module
                        parts.add(new SparePart("p15", "Camera Module", 6, 3200.0, "colombo"));
                        parts.add(new SparePart("p16", "Camera Module", 4, 3200.0, "galle"));

                        // Speaker Module
                        parts.add(new SparePart("p17", "Speaker Module", 10, 1200.0, "colombo"));
                        parts.add(new SparePart("p18", "Speaker Module", 8, 1200.0, "galle"));

                        // Laptop Battery
                        parts.add(new SparePart("p19", "Laptop Battery", 5, 8500.0, "colombo"));
                        parts.add(new SparePart("p20", "Laptop Battery", 3, 8500.0, "galle"));

                        // Wi-Fi Antenna Module
                        parts.add(new SparePart("p21", "Wi-Fi Antenna Module", 8, 1500.0, "colombo"));
                        parts.add(new SparePart("p22", "Wi-Fi Antenna Module", 0, 1500.0, "galle")); // Out of stock at Galle!

                        for (SparePart p : parts) {
                            db.collection("spareParts").document(p.getPartId()).set(p);
                        }
                    }
                }
                onComplete.run();
            });
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
