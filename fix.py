import re
with open('src/main/java/com/example/bookstore/service/cluster/CustomerAnalysisService.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_str = """        // 3. Gọi ML API
        PredictionResult result = mlApiService.predictCustomer(input);

        // 4. Cập nhật kết quả vào entity"""

new_str = """        // 3. Gọi ML API
        PredictionResult result;
        try {
            result = mlApiService.predictCustomer(input);
        } catch (Exception e) {
            log.warn("ML API prediction failed for customer {}: {}", customer.getId(), e.getMessage());
            return customerService.save(customer);
        }

        // 4. Cập nhật kết quả vào entity"""

new_content = content.replace(old_str, new_str)
with open('src/main/java/com/example/bookstore/service/cluster/CustomerAnalysisService.java', 'w', encoding='utf-8') as f:
    f.write(new_content)
