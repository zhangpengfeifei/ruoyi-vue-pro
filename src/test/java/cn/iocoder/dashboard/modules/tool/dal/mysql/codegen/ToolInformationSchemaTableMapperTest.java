package cn.iocoder.dashboard.modules.tool.dal.mysql.codegen;

import cn.iocoder.dashboard.BaseDbUnitTest;
import cn.iocoder.dashboard.modules.tool.dal.dataobject.codegen.ToolSchemaTableDO;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolInformationSchemaTableMapperTest extends BaseDbUnitTest {

    @Resource
    private ToolSchemaTableMapper toolInformationSchemaTableMapper;

    @Test
    public void tstSelectListByTableSchema() {
        List<ToolSchemaTableDO> tables = toolInformationSchemaTableMapper
                .selectListByTableSchema("ruoyi-vue-pro");
        assertTrue(tables.size() > 0);
    }

}
