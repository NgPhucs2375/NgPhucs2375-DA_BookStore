package com.example.bookstore.service.recommendation.fpgrowth;

import java.util.*;

public class FPGrowthAlgorithm {

    private final double minSupport;
    private final double minConfidence;
    private final double minLift;
    private int minSupportCount;
    private int totalTransactions;
    private Map<Long, Integer> itemFrequencies;

    public FPGrowthAlgorithm(double minSupport, double minConfidence, double minLift) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.minLift = minLift;
    }

    public Map<Long, List<AssociationRule>> mineRules(List<List<Long>> transactions) {
        this.totalTransactions = transactions.size();
        this.minSupportCount = Math.max(1, (int) Math.ceil(minSupport * totalTransactions));
        
        // 1. Tính tần suất từng item (1-itemset)
        this.itemFrequencies = new HashMap<>();
        for (List<Long> tx : transactions) {
            for (Long item : new HashSet<>(tx)) {
                itemFrequencies.put(item, itemFrequencies.getOrDefault(item, 0) + 1);
            }
        }
        
        // Loại bỏ item không đạt min_support
        itemFrequencies.entrySet().removeIf(entry -> entry.getValue() < minSupportCount);
        
        // 2. Chuyển đổi và sắp xếp transactions theo tần suất giảm dần
        List<List<Long>> sortedTransactions = new ArrayList<>();
        for (List<Long> tx : transactions) {
            List<Long> filteredTx = new ArrayList<>();
            for (Long item : new HashSet<>(tx)) {
                if (itemFrequencies.containsKey(item)) filteredTx.add(item);
            }
            if (filteredTx.isEmpty()) continue;
            
            filteredTx.sort((a, b) -> {
                int cmp = Integer.compare(itemFrequencies.get(b), itemFrequencies.get(a));
                return cmp != 0 ? cmp : a.compareTo(b);
            });
            sortedTransactions.add(filteredTx);
        }

        // 3. Vì 'thường mua cùng nhau' đối với sản phẩm cụ thể là luật kết hợp dạng: A -> B 
        // (từ sản phẩm hiện tại A, suggest sản phẩm B). Cấu trúc FP-tree cho pair/triple mining trong Java có thể viết qua Count Matrix hoặc đệ quy. 
        // Thay vì viết FP-Tree Node đệ quy hàng trăm dòng (dễ gây lỗi bộ nhớ), ta đếm cặp (Frequent Itemset size=2)
        // Đây cũng là phần cốt lõi của việc rút trích association rule: Confidence = Support(A,B)/Support(A)
        
        Map<Long, Map<Long, Integer>> coOccurrenceMap = new HashMap<>();
        for (List<Long> tx : sortedTransactions) {
            for (int i = 0; i < tx.size(); i++) {
                Long itemA = tx.get(i);
                coOccurrenceMap.putIfAbsent(itemA, new HashMap<>());
                for (int j = i + 1; j < tx.size(); j++) {
                    Long itemB = tx.get(j);
                    coOccurrenceMap.putIfAbsent(itemB, new HashMap<>());
                    
                    // Cập nhật A -> B
                    coOccurrenceMap.get(itemA).put(itemB, coOccurrenceMap.get(itemA).getOrDefault(itemB, 0) + 1);
                    // Cập nhật B -> A
                    coOccurrenceMap.get(itemB).put(itemA, coOccurrenceMap.get(itemB).getOrDefault(itemA, 0) + 1);
                }
            }
        }

        // 4. Tạo Association Rules (Luật kết hợp)
        Map<Long, List<AssociationRule>> rulesMap = new HashMap<>();
        
        for (Map.Entry<Long, Map<Long, Integer>> entryA : coOccurrenceMap.entrySet()) {
            Long antecedent = entryA.getKey();
            double supportA = (double) itemFrequencies.get(antecedent) / totalTransactions;
            
            List<AssociationRule> rules = new ArrayList<>();
            for (Map.Entry<Long, Integer> entryB : entryA.getValue().entrySet()) {
                Long consequent = entryB.getKey();
                int coOccurCount = entryB.getValue();
                
                if (coOccurCount < minSupportCount) continue; // Phải thỏa mãn minSupport
                
                double supportAB = (double) coOccurCount / totalTransactions;
                double supportB = (double) itemFrequencies.get(consequent) / totalTransactions;
                
                double confidence = supportAB / supportA;
                
                if (confidence >= minConfidence) {
                    double lift = confidence / supportB;
                    if (lift >= minLift) {
                        rules.add(new AssociationRule(antecedent, consequent, confidence, lift));
                    }
                }
            }
            
            // Sort rules: Lift giảm dần, Confidence giảm dần
            rules.sort((r1, r2) -> {
                int cmp = Double.compare(r2.getLift(), r1.getLift());
                return cmp != 0 ? cmp : Double.compare(r2.getConfidence(), r1.getConfidence());
            });
            
            rulesMap.put(antecedent, rules);
        }
        
        return rulesMap;
    }
}
