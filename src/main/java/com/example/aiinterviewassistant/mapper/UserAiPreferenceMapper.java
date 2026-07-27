package com.example.aiinterviewassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiinterviewassistant.entity.UserAiPreference;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface UserAiPreferenceMapper extends BaseMapper<UserAiPreference> {

    @Insert("""
            INSERT INTO user_ai_preference (user_id, ai_model_id)
            VALUES (#{preference.userId}, #{preference.aiModelId})
            ON DUPLICATE KEY UPDATE
                ai_model_id = #{preference.aiModelId},
                update_time = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("preference") UserAiPreference preference);
}
