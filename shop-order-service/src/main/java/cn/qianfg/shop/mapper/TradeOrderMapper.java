package cn.qianfg.shop.mapper;

import cn.qianfg.shop.pojo.TradeOrder;
import cn.qianfg.shop.pojo.TradeOrderExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradeOrderMapper {
    int countByExample(TradeOrderExample example);

    int deleteByExample(TradeOrderExample example);

    int deleteByPrimaryKey(Long orderId);

    int insert(TradeOrder record);

    int insertSelective(TradeOrder record);

    List<TradeOrder> selectByExample(TradeOrderExample example);

    TradeOrder selectByPrimaryKey(Long orderId);

    int updateByExampleSelective(@Param("record") TradeOrder record, @Param("example") TradeOrderExample example);

    int updateByExample(@Param("record") TradeOrder record, @Param("example") TradeOrderExample example);

    int updateByPrimaryKeySelective(TradeOrder record);

    int updateByPrimaryKey(TradeOrder record);
}