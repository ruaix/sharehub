package app.sharehub.mapper;

import app.sharehub.domain.MembershipEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MembershipMapper extends BaseMapper<MembershipEntity> {}
