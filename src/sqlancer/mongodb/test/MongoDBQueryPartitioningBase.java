package sqlancer.mongodb.test;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.ast.newast.Node;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.common.oracle.TernaryLogicPartitioningOracleBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.mongodb.MongoDBProvider.MongoDBGlobalState;
import sqlancer.mongodb.MongoDBSchema;
import sqlancer.mongodb.MongoDBSchema.MongoDBColumn;
import sqlancer.mongodb.MongoDBSchema.MongoDBTable;
import sqlancer.mongodb.MongoDBSchema.MongoDBTables;
import sqlancer.mongodb.ast.MongoDBExpression;
import sqlancer.mongodb.ast.MongoDBSelect;
import sqlancer.mongodb.gen.MongoDBExpressionGenerator;

public class MongoDBQueryPartitioningBase
        extends TernaryLogicPartitioningOracleBase<Node<MongoDBExpression>, MongoDBGlobalState> implements TestOracle {

    protected MongoDBSchema schema;
    protected MongoDBTables targetTables;
    protected MongoDBTable mainTable;
    protected List<MongoDBColumnTestReference> targetColumns;
    protected MongoDBExpressionGenerator expressionGenerator;
    protected MongoDBSelect<MongoDBExpression> select;

    public MongoDBQueryPartitioningBase(MongoDBGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws Exception {
        schema = state.getSchema();
        targetTables = schema.getRandomTableNonEmptyTables();
        mainTable = targetTables.getTables().get(0);
        generateTargetColumns();
        expressionGenerator = new MongoDBExpressionGenerator().setColumns(targetColumns);
        initializeTernaryPredicateVariants();
        select = new MongoDBSelect<>(mainTable.getName(), targetColumns.get(0));
        select.setProjectionList(targetColumns);
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setLookupList(targetColumns);
        } else {
            select.setLookupList(Randomly.nonEmptySubset(targetColumns));
        }
    }

    private void generateTargetColumns() {
        targetColumns = new ArrayList<>();
        for (MongoDBColumn c : mainTable.getColumns()) {
            targetColumns.add(new MongoDBColumnTestReference(c, true));
        }
        List<MongoDBColumnTestReference> joinsOtherTables = new ArrayList<>();
        for (int i = 1; i < targetTables.getTables().size(); i++) {
            MongoDBTable procTable = targetTables.getTables().get(i);
            for (MongoDBColumn c : procTable.getColumns()) {
                joinsOtherTables.add(new MongoDBColumnTestReference(c, false));
            }
        }
        if (!joinsOtherTables.isEmpty()) {
            int randNumber = state.getRandomly().getInteger(1, Math.min(joinsOtherTables.size(), 4));
            List<MongoDBColumnTestReference> subsetJoinsOtherTables = Randomly.nonEmptySubset(joinsOtherTables,
                    randNumber);
            targetColumns.addAll(subsetJoinsOtherTables);
        }
    }

    @Override
    protected ExpressionGenerator<Node<MongoDBExpression>> getGen() {
        return expressionGenerator;
    }
}
