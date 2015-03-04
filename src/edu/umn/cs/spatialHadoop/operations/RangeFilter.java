package edu.umn.cs.spatialHadoop.operations;

import org.apache.hadoop.mapred.JobConf;

import edu.umn.cs.spatialHadoop.OperationsParams;
import edu.umn.cs.spatialHadoop.core.GlobalIndex;
import edu.umn.cs.spatialHadoop.core.Partition;
import edu.umn.cs.spatialHadoop.core.Rectangle;
import edu.umn.cs.spatialHadoop.core.ResultCollector;
import edu.umn.cs.spatialHadoop.core.Shape;
import edu.umn.cs.spatialHadoop.mapred.DefaultBlockFilter;

/**
 * A filter function that selects partitions overlapping with a query range.
 * @author Ahmed Eldawy
 *
 */
public class RangeFilter extends DefaultBlockFilter {
  /**Configuration parameter for setting a search query range*/
  public static final String QueryRange = "SpatialInputFormat.QueryRange";

  /**A shape that is used to filter input*/
  private Shape queryRange;
  
  @Override
  public void configure(JobConf job) {
    this.queryRange = OperationsParams.getShape(job, QueryRange);
  }
  
  @Override
  public void selectCells(GlobalIndex<Partition> gIndex,
      ResultCollector<Partition> output) {
    int numPartitions;
    if (gIndex.isReplicated()) {
      // Need to process all partitions to perform duplicate avoidance
      numPartitions = gIndex.rangeQuery(queryRange, output);
      RangeQuery.LOG.info("Selected "+numPartitions+" partitions overlapping "+queryRange);
    } else {
      Rectangle queryMBR = this.queryRange.getMBR();
      // Need to process only partitions on the perimeter of the query range
      // Partitions that are totally contained in query range should not be
      // processed and should be copied to output directly
      numPartitions = 0;
      for (Partition p : gIndex) {
        if (queryMBR.contains(p)) {
          // TODO partitions totally contained in query range should be copied
          // to output directly

          // XXX Until hard links are supported, R-tree blocks are processed
          // similar to R+-tree
          if (p.isIntersected(queryRange)) {
            output.collect(p);
            numPartitions++;
          }
        } else if (p.isIntersected(queryMBR) && p.isIntersected(queryRange)) {
          output.collect(p);
          numPartitions++;
        }
      }
      RangeQuery.LOG.info("Selected "+numPartitions+" partitions on the perimeter of "+queryMBR);
    }
  }
}