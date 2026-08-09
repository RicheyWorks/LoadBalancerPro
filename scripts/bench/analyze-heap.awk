NR > 1 {
    if ($2 <= 0) invalid = 1
    value[count++] = $2
}

END {
    if (count < 12) {
        print "At least 12 heap samples are required" > "/dev/stderr"
        exit 1
    }
    if (invalid) {
        print "Every heap sample must be a positive byte count" > "/dev/stderr"
        exit 1
    }
    buckets = 6
    width = int(count / buckets)
    for (bucket = 0; bucket < buckets; bucket++) {
        start = bucket * width
        finish = (bucket == buckets - 1) ? count : start + width
        minimum = value[start]
        for (sample_index = start + 1; sample_index < finish; sample_index++) {
            if (value[sample_index] < minimum) minimum = value[sample_index]
        }
        x = bucket
        y = minimum
        floor[bucket] = minimum
        sum_x += x
        sum_y += y
        sum_xy += x * y
        sum_xx += x * x
    }
    denominator = buckets * sum_xx - sum_x * sum_x
    slope = denominator == 0 ? 0 : (buckets * sum_xy - sum_x * sum_y) / denominator
    projected = slope * (buckets - 1)
    observed = floor[buckets - 1] - floor[0]
    pass = projected <= budget && observed <= budget
    printf "{\"sampleCount\":%d,\"bucketCount\":%d,\"firstFloorBytes\":%.0f,\"lastFloorBytes\":%.0f,\"observedFloorGrowthBytes\":%.0f,\"projectedTrendBytes\":%.0f,\"growthBudgetBytes\":%.0f,\"pass\":%s}\n", count, buckets, floor[0], floor[buckets - 1], observed, projected, budget, pass ? "true" : "false" > output
    if (!pass) {
        print "Heap post-GC floor trend exceeded the local growth budget" > "/dev/stderr"
        exit 1
    }
}
