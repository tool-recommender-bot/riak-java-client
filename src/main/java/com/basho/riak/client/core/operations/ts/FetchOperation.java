package com.basho.riak.client.core.operations.ts;

import com.basho.riak.client.core.operations.PBFutureOperation;
import com.basho.riak.client.core.query.timeseries.Cell;
import com.basho.riak.client.core.query.timeseries.CollectionConverters;
import com.basho.riak.client.core.query.timeseries.QueryResult;
import com.basho.riak.client.core.query.timeseries.PbResultFactory;
import com.basho.riak.client.core.util.BinaryValue;
import com.basho.riak.protobuf.RiakTsPB;
import com.basho.riak.protobuf.RiakMessageCodes;
import com.google.protobuf.ByteString;

import java.util.Collection;
import java.util.List;

/**
 * An operation to fetch a single row in a Riak Time Series table.
 *
 * @author Alex Moore <amoore at basho dot com>
 * @author Sergey Galkin <srggal at gmail dot com>
 * @since 2.0.3
 */
public class FetchOperation extends PBFutureOperation<QueryResult, RiakTsPB.TsGetResp, BinaryValue>
{
    private final Builder builder;
    private BinaryValue queryInfoMessage;

    private FetchOperation(Builder builder)
    {
        super(RiakMessageCodes.MSG_TsGetReq,
              RiakMessageCodes.MSG_TsGetResp,
              builder.reqBuilder,
              RiakTsPB.TsGetResp.PARSER);

        this.builder = builder;
    }

    @Override
    protected QueryResult convert(List<RiakTsPB.TsGetResp> responses)
    {
        // This is not a streaming op, there will only be one response
        final RiakTsPB.TsGetResp response = checkAndGetSingleResponse(responses);

        return PbResultFactory.convertPbGetResp(response);
    }

    @Override
    public BinaryValue getQueryInfo()
    {
        if (this.queryInfoMessage == null)
        {
            this.queryInfoMessage = createQueryInfoMessage();
        }

        return this.queryInfoMessage;
    }

    private BinaryValue createQueryInfoMessage()
    {
        final StringBuilder sb = new StringBuilder();

        for (Cell cell: this.builder.keyValues)
        {
            if (sb.length() > 0)
            {
                sb.append(", ");
            }
            sb.append( cell == null ? "NULL" : cell.toString());
        }

        return BinaryValue.create(
                String.format("SELECT * FROM %s  WHERE PRIMARY KEY = { %s }",
                        this.builder.tableName, sb.toString())
            );
    }

    public static class Builder
    {
        private final BinaryValue tableName;
        private final Collection<Cell> keyValues;

        private final RiakTsPB.TsGetReq.Builder reqBuilder = RiakTsPB.TsGetReq.newBuilder();

        public Builder(BinaryValue tableName, Collection<Cell> keyValues)
        {
            if (tableName == null || tableName.length() == 0)
            {
                throw new IllegalArgumentException("Table Name cannot be null or an empty string.");
            }

            if (keyValues == null || keyValues.size() == 0)
            {
                throw new IllegalArgumentException("Key Values cannot be null or an empty list.");
            }

            this.reqBuilder.setTable(ByteString.copyFrom(tableName.getValue()));
            this.reqBuilder.addAllKey(CollectionConverters.wrapAsIterablePBCell(keyValues.iterator()));

            this.tableName = tableName;
            this.keyValues = keyValues;
        }

        public Builder withTimeout(int timeout)
        {
            this.reqBuilder.setTimeout(timeout);
            return this;
        }

        public FetchOperation build()
        {
            return new FetchOperation(this);
        }
    }
}
