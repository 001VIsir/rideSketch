#!/usr/bin/env python3
"""
简单的知识库服务 - 不依赖Chroma向量库
使用关键词匹配进行知识检索

使用方法:
1. 启动服务: python chroma_service.py
2. API端点:
   - POST /add - 添加文档
   - POST /query - 查询相似文档
   - GET  /health - 健康检查
"""

from flask import Flask, request, jsonify
import os

app = Flask(__name__)

# 知识库存储
knowledge_base = []
next_id = 1

# 预置知识库
INITIAL_KNOWLEDGE = [
    {
        "id": "tech_1",
        "content": """骑行技巧：1.保持正确的骑行姿势，身体略微前倾约15-30度。2.踏频保持在80-100 RPM之间最省力。3.爬坡时使用轻齿轮，sit/stand交替发力。4.下坡时控制速度，优先使用后刹车。5.长时间骑行记得补充水分和电解质，每15-20分钟补充一次。""",
        "category": "骑行技巧"
    },
    {
        "id": "safety_1",
        "content": """骑行安全注意事项：1.始终佩戴头盔，选择合适尺寸并系紧。2.夜间骑行必须安装前后灯，前灯白色，后灯红色。3.遵守交通规则，使用手势信号指示转弯和停车。4.定期检查刹车系统、轮胎气压和链条状况。5.避免戴耳机骑车，保持注意力集中。6.与前车保持安全距离，特别是下坡时。7.恶劣天气尽量减少外出。""",
        "category": "骑行安全"
    },
    {
        "id": "route_1",
        "content": """北京经典骑行路线：1.天安门-故宫-景山：适合初学者，路程短，约5公里，沿途可欣赏皇城风光。2.长安街：沿着长安街骑行，经过天安门广场，是北京最具代表性的骑行路线。3.奥林匹克公园：绕鸟巢、水立方骑行一圈约8公里，夜景优美，适合休闲骑。4.妙峰山：经典爬坡路线，全长约20公里，海拔上升约600米，风景优美。5.什刹海-胡同：适合休闲骑行，穿梭老北京胡同，观赏什刹海风光。6.温榆河绿道：新建成的骑行道，全长约30公里，适合长距离训练。""",
        "category": "北京骑行路线"
    },
    {
        "id": "gear_1",
        "content": """骑行必备装备清单：1.头盔：保护头部安全，必不可少，选择合适尺寸。2.手套：防滑、减震，保护手部，长距离骑行必备。3.骑行裤：带坐垫的骑行裤可减少摩擦，提高舒适度。4.车灯：夜间骑行必备，前灯白光，后灯红光。5.水壶：保持水分补充，建议使用专用骑行水壶。6.维修工具：备胎、充气筒、多功能工具、补胎工具。7.码表：记录骑行数据，包括速度、距离、海拔等。8.防晒装备：骑行眼镜、防晒霜、空顶帽。""",
        "category": "骑行装备"
    },
    {
        "id": "training_1",
        "content": """初学者训练计划：第一周：每次骑行30分钟，距离10-15公里，强度适中。第二周：每次骑行45分钟，距离15-20公里，逐步适应。第三周：每次骑行60分钟，距离25-30公里，开始有氧训练。第四周：尝试更长距离，逐步增加强度至每次80分钟。训练要点：每次骑行前热身5-10分钟，活动关节和拉伸肌肉。保持均匀踏频，不要忽快忽慢。骑行结束后放松拉伸10-15分钟。循序渐进，避免过度训练导致受伤。每周至少休息1-2天，让身体恢复。""",
        "category": "训练计划"
    },
    {
        "id": "nutrition_1",
        "content": """骑行营养补给：1.水分：每小时补充200-300ml水，长距离骑行可补充运动饮料。2.碳水化合物：骑行前吃面包、香蕉等，骑行中可补充能量棒。3.电解质：长时间骑行需要补充钠、钾、镁等电解质。4.蛋白质：骑行后补充蛋白质帮助肌肉恢复。5.避免：骑行前避免高脂肪、高纤维食物。""",
        "category": "营养补给"
    },
    {
        "id": "maintenance_1",
        "content": """自行车保养维护：1.每次骑行后清洁链条并加油。2.每周检查轮胎气压，保持适当胎压。3.每月检查刹车片磨损情况，及时更换。4.每季度检查变速系统，调整换挡精度。5.每年进行全面保养，包括花鼓、中轴等内部保养。6.存放时避免阳光直射和潮湿环境。""",
        "category": "车辆保养"
    }
]


def initialize_knowledge_base():
    """初始化知识库"""
    global knowledge_base, next_id
    if knowledge_base:
        print(f"知识库已有 {len(knowledge_base)} 条文档，跳过初始化")
        return

    for item in INITIAL_KNOWLEDGE:
        knowledge_base.append(item)

    next_id = len(INITIAL_KNOWLEDGE) + 1
    print(f"知识库初始化完成，共添加 {len(knowledge_base)} 条文档")


# 初始化知识库
initialize_knowledge_base()


def keyword_search(query, top_k=3):
    """简单的关键词匹配搜索"""
    query_lower = query.lower()
    scores = []

    for doc in knowledge_base:
        content_lower = doc["content"].lower()
        # 计算简单的关键词匹配分数
        score = 0
        # 关键词匹配
        keywords = query_lower.split()
        for kw in keywords:
            if kw in content_lower:
                score += 1
            # 也匹配类别
            if kw in doc.get("category", "").lower():
                score += 0.5
        # 长度惩罚（偏好较短的匹配）
        score += len(content_lower) / 1000
        scores.append((doc, score))

    # 按分数排序
    scores.sort(key=lambda x: x[1], reverse=True)
    return [doc for doc, score in scores[:top_k]]


@app.route('/health', methods=['GET'])
def health():
    """健康检查"""
    return jsonify({
        "status": "ok",
        "service": "knowledge-base",
        "document_count": len(knowledge_base)
    })


@app.route('/add', methods=['POST'])
def add_documents():
    """添加文档到知识库"""
    global next_id

    data = request.json
    documents = data.get('documents', [])
    ids = data.get('ids', None)
    metadatas = data.get('metadatas', [])

    if not documents:
        return jsonify({"error": "documents is required"}), 400

    added = []
    for i, doc in enumerate(documents):
        doc_id = ids[i] if ids and i < len(ids) else f"doc_{next_id}"
        metadata = metadatas[i] if metadatas and i < len(metadatas) else {}

        knowledge_base.append({
            "id": doc_id,
            "content": doc,
            "category": metadata.get("category", "未分类")
        })
        added.append(doc_id)
        next_id += 1

    return jsonify({
        "success": True,
        "added_count": len(added),
        "ids": added
    })


@app.route('/query', methods=['POST'])
def query_documents():
    """查询相似文档 - 使用关键词匹配"""
    data = request.json
    query_text = data.get('query', '')
    n_results = data.get('n_results', 3)

    if not query_text:
        return jsonify({"error": "query is required"}), 400

    # 关键词搜索
    results = keyword_search(query_text, n_results)

    response_data = []
    for doc in results:
        response_data.append({
            "id": doc["id"],
            "content": doc["content"],
            "metadata": {"category": doc.get("category", "")},
            "distance": 0.5  # 模拟距离
        })

    return jsonify({
        "success": True,
        "results": response_data
    })


@app.route('/count', methods=['GET'])
def count_documents():
    """获取文档数量"""
    return jsonify({
        "count": len(knowledge_base)
    })


@app.route('/reset', methods=['POST'])
def reset_collection():
    """重置知识库"""
    global knowledge_base, next_id
    knowledge_base = []
    next_id = 1
    initialize_knowledge_base()
    return jsonify({"success": True, "message": "知识库已重置"})


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    print(f"\n=== 知识库服务启动 ===")
    print(f"服务地址: http://localhost:{port}")
    print(f"知识库文档数: {len(knowledge_base)}")
    print(f"========================\n")

    app.run(host='0.0.0.0', port=port, debug=False)
