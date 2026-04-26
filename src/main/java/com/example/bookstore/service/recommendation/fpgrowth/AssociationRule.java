package com.example.bookstore.service.recommendation.fpgrowth;

public class AssociationRule {
    private final Long antecedent;
    private final Long consequent;
    private final double confidence;
    private final double lift;

    public AssociationRule(Long antecedent, Long consequent, double confidence, double lift) {
        this.antecedent = antecedent;
        this.consequent = consequent;
        this.confidence = confidence;
        this.lift = lift;
    }

    public Long getAntecedent() { return antecedent; }
    public Long getConsequent() { return consequent; }
    public double getConfidence() { return confidence; }
    public double getLift() { return lift; }
}
