"""
无人机损伤检测 API 服务
基于 YOLOv8 模型进行无人机损伤检测
"""
import os
import sys
import json
import time
import cv2
import numpy as np
from pathlib import Path
from datetime import datetime
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
from werkzeug.utils import secure_filename
from ultralytics import YOLO

# 添加 YOLOv8 项目路径
# detection-api 在 drone-rental-system 下，需要向上两级到达 Drone Rental System
yolo_project_path = Path(__file__).parent.parent.parent / 'YoloV8_Dronedetection01'
sys.path.insert(0, str(yolo_project_path))

app = Flask(__name__)
CORS(app)  # 允许跨域请求

# 配置
UPLOAD_FOLDER = Path(__file__).parent / 'uploads'
RESULT_FOLDER = Path(__file__).parent / 'results'
# 使用新训练的损伤检测模型
MODEL_PATH = yolo_project_path / 'weights' / 'damage_detector' / 'train' / 'weights' / 'best.pt'

# 创建必要的目录
UPLOAD_FOLDER.mkdir(exist_ok=True)
RESULT_FOLDER.mkdir(exist_ok=True)

app.config['UPLOAD_FOLDER'] = str(UPLOAD_FOLDER)
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024  # 50MB 最大文件大小

# 允许的文件扩展名
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'mp4', 'avi', 'mov'}

# 全局模型变量
_model = None


def get_model():
    """加载 YOLOv8 模型（单例模式）"""
    global _model
    if _model is None:
        print(f"[YOLO] 当前目录: {Path(__file__).parent}")
        print(f"[YOLO] 项目路径: {yolo_project_path}")
        print(f"[YOLO] 模型路径: {MODEL_PATH}")
        print(f"[YOLO] 模型存在: {MODEL_PATH.exists()}")
        if not MODEL_PATH.exists():
            raise FileNotFoundError(f"模型文件不存在: {MODEL_PATH}")
        print(f"[YOLO] 开始加载模型...")
        _model = YOLO(str(MODEL_PATH))
        print("[YOLO] 模型加载成功!")
    return _model


def allowed_file(filename):
    """检查文件扩展名是否允许"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


def classify_damage(detections):
    """
    根据检测结果分类损伤类型和判定责任
    基于 Aircraft Surface Defect Detection 数据集训练的模型

    类别映射:
        0: 裂纹 (crack)
        1: 凹陷 (dent)
        2: 零件破损 (missing-head)
        3: 漆面损伤 (paint-off)
        4: 刮痕 (scratch)

    Args:
        detections: YOLOv8 检测结果列表

    Returns:
        dict: 包含损伤类型、严重程度和责任判定的字典
    """
    # 损伤类型定义 - 基于飞机表面缺陷数据集的5个类别
    damage_types = {
        # 裂纹 - 严重损伤，通常是撞击造成
        0: {'name': '裂纹', 'severity': 'severe', 'responsibility': 'user'},

        # 凹陷 - 中等损伤，通常是撞击或压力造成
        1: {'name': '凹陷', 'severity': 'moderate', 'responsibility': 'user'},

        # 零件破损 - 严重损伤，可能是使用不当或质量问题
        2: {'name': '零件破损', 'severity': 'severe', 'responsibility': 'shared'},

        # 漆面损伤 - 轻微损伤，通常是正常磨损
        3: {'name': '漆面损伤', 'severity': 'minor', 'responsibility': 'user'},

        # 刮痕 - 轻微损伤，通常是正常使用磨损
        4: {'name': '刮痕', 'severity': 'minor', 'responsibility': 'user'},
    }

    detected_damages = []

    for det in detections:
        class_id = int(det.get('class', 0))
        confidence = det.get('confidence', 0)

        # 根据检测类别确定损伤类型
        if class_id in damage_types:
            damage_type = damage_types[class_id]
            detected_damages.append({
                'type': damage_type['name'],
                'severity': damage_type['severity'],
                'responsibility': damage_type['responsibility'],
                'confidence': confidence,
                'bbox': det.get('bbox', [])
            })

    # 如果没有检测到任何损伤
    if len(detected_damages) == 0:
        return {
            'detected_damages': [],
            'overall_severity': 'none',
            'responsibility': 'none',
            'responsibility_reason': '无人机外观完好，未检测到明显损伤',
            'damage_count': 0
        }

    # 统计各类责任的数量和严重程度
    user_damages = [d for d in detected_damages if d['responsibility'] == 'user']
    operator_damages = [d for d in detected_damages if d['responsibility'] == 'operator']
    shared_damages = [d for d in detected_damages if d['responsibility'] == 'shared']

    # 确定最严重的损伤等级
    severity_order = {'none': 0, 'minor': 1, 'moderate': 2, 'severe': 3}
    max_severity = 'none'
    for damage in detected_damages:
        if severity_order.get(damage['severity'], 0) > severity_order.get(max_severity, 0):
            max_severity = damage['severity']

    # 责任判定逻辑（更细致）
    responsibility = 'unknown'
    responsibility_reason = ''

    # 1. 既有共享损伤又有其他损伤
    if shared_damages and (user_damages or operator_damages):
        responsibility = 'shared'
        responsibility_reason = '检测到严重零件破损，可能由用户使用和设备因素共同造成'

    # 2. 只有共享损伤
    elif shared_damages:
        responsibility = 'shared'
        responsibility_reason = '检测到零件破损，需要进一步检查确定具体原因'

    # 3. 既有用户责任又有运营方责任损伤
    elif user_damages and operator_damages:
        # 计算严重程度权重
        user_severity = sum(severity_order.get(d['severity'], 0) for d in user_damages)
        operator_severity = sum(severity_order.get(d['severity'], 0) for d in operator_damages)

        if user_severity > operator_severity * 1.5:
            responsibility = 'user'
            responsibility_reason = f'检测到{len(user_damages)}处用户使用造成的损伤，程度较重'
        elif operator_severity > user_severity * 1.5:
            responsibility = 'operator'
            responsibility_reason = f'检测到{len(operator_damages)}处设备故障，可能是维护问题'
        else:
            responsibility = 'shared'
            responsibility_reason = '用户使用损伤和设备故障同时存在'

    # 4. 只有用户责任损伤
    elif user_damages:
        # 根据损伤数量和程度判断
        severe_count = sum(1 for d in user_damages if d['severity'] == 'severe')
        if severe_count > 0:
            responsibility = 'user'
            responsibility_reason = f'检测到{severe_count}处严重损伤，用户使用不当造成'
        elif len(user_damages) >= 3:
            responsibility = 'user'
            responsibility_reason = f'检测到{len(user_damages)}处损伤，累积使用损伤'
        else:
            responsibility = 'user'
            responsibility_reason = '检测到用户使用造成的轻微损伤'

    # 5. 只有运营方责任损伤
    elif operator_damages:
        responsibility = 'operator'
        responsibility_reason = '检测到设备内部故障，属于运营方维护责任'

    return {
        'detected_damages': detected_damages,
        'overall_severity': max_severity,
        'responsibility': responsibility,
        'responsibility_reason': responsibility_reason,
        'damage_count': len(detected_damages)
    }


@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'ok',
        'service': 'Drone Damage Detection API',
        'model_loaded': _model is not None
    })


@app.route('/api/detect/images', methods=['POST'])
def detect_images():
    """
    多图片批量损伤检测接口

    请求参数:
        - files: 多个上传的图片文件 (支持 1-6 张)
        - vehicle_id: 无人机ID (可选)
        - order_id: 订单ID (可选)

    返回:
        JSON 格式的综合检测结果
    """
    if 'files' not in request.files:
        return jsonify({'code': 400, 'message': '没有上传文件'}), 400

    files = request.files.getlist('files')
    if not files or all(f.filename == '' for f in files):
        return jsonify({'code': 400, 'message': '未选择文件'}), 400

    # 过滤有效文件
    valid_files = [f for f in files if f.filename and allowed_file(f.filename)]

    if len(valid_files) == 0:
        return jsonify({'code': 400, 'message': '没有有效的图片文件'}), 400

    if len(valid_files) > 6:
        return jsonify({'code': 400, 'message': '最多支持上传6张图片'}), 400

    try:
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        model = get_model()

        all_detections = []
        image_results = []
        total_inference_time = 0

        for idx, file in enumerate(valid_files):
            # 保存上传的文件
            filename = secure_filename(f"{timestamp}_{idx}_{file.filename}")
            filepath = UPLOAD_FOLDER / filename
            file.save(filepath)

            # 读取图片
            image = cv2.imread(str(filepath))
            if image is None:
                continue

            # 执行检测
            start_time = time.time()
            results = model(image, conf=0.25, iou=0.45)
            inference_time = time.time() - start_time
            total_inference_time += inference_time

            # 处理单张图片的检测结果
            detections = []
            if len(results) > 0 and results[0].boxes is not None:
                boxes = results[0].boxes
                for box in boxes:
                    x1, y1, x2, y2 = box.xyxy[0].tolist()
                    cls = int(box.cls[0])
                    conf = float(box.conf[0])

                    detections.append({
                        'class': cls,
                        'confidence': conf,
                        'bbox': [x1, y1, x2, y2]
                    })

            # 保存带标注的图片
            result_image = results[0].plot() if len(results) > 0 else image
            result_filename = f"result_{filename}"
            result_filepath = RESULT_FOLDER / result_filename
            cv2.imwrite(str(result_filepath), result_image)

            image_results.append({
                'index': idx,
                'image_path': f"/detection-api/uploads/{filename}",
                'result_path': f"/detection-api/results/{result_filename}",
                'detections': detections,
                'damage_count': len(detections),
                'inference_time': round(inference_time, 3)
            })

            all_detections.extend(detections)

        # 综合损伤分类和责任判定
        damage_analysis = classify_damage(all_detections)

        # 获取车辆ID和订单ID
        vehicle_id = request.form.get('vehicle_id', '')
        order_id = request.form.get('order_id', '')

        return jsonify({
            'code': 200,
            'message': f'检测完成，共处理 {len(valid_files)} 张图片',
            'data': {
                'vehicle_id': vehicle_id,
                'order_id': order_id,
                'detection_id': f"DET_{timestamp}",
                'image_count': len(valid_files),
                'image_results': image_results,
                'total_detections': all_detections,
                'damage_analysis': damage_analysis,
                'total_inference_time': round(total_inference_time, 3),
                'model_name': 'yolov8_drone_damage',
                'timestamp': timestamp
            }
        })

    except Exception as e:
        print(f"批量检测失败: {e}")
        return jsonify({'code': 500, 'message': f'检测失败: {str(e)}'}), 500


@app.route('/api/detect/image', methods=['POST'])
def detect_image():
    """
    图片损伤检测接口

    请求参数:
        - file: 上传的图片文件
        - vehicle_id: 无人机ID (可选)
        - order_id: 订单ID (可选)

    返回:
        JSON 格式的检测结果
    """
    if 'file' not in request.files:
        return jsonify({'code': 400, 'message': '没有上传文件'}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({'code': 400, 'message': '未选择文件'}), 400

    if not allowed_file(file.filename):
        return jsonify({'code': 400, 'message': '不支持的文件格式'}), 400

    try:
        # 保存上传的文件
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = secure_filename(f"{timestamp}_{file.filename}")
        filepath = UPLOAD_FOLDER / filename
        file.save(filepath)

        # 读取图片
        image = cv2.imread(str(filepath))
        if image is None:
            return jsonify({'code': 400, 'message': '图片读取失败'}), 400

        # 执行检测
        start_time = time.time()
        model = get_model()
        results = model(image, conf=0.25, iou=0.45)
        inference_time = time.time() - start_time

        # 处理检测结果
        detections = []
        if len(results) > 0 and results[0].boxes is not None:
            boxes = results[0].boxes
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                cls = int(box.cls[0])
                conf = float(box.conf[0])

                detections.append({
                    'class': cls,
                    'confidence': conf,
                    'bbox': [x1, y1, x2, y2]
                })

        # 保存带标注的图片
        result_image = results[0].plot() if len(results) > 0 else image
        result_filename = f"result_{filename}"
        result_filepath = RESULT_FOLDER / result_filename
        cv2.imwrite(str(result_filepath), result_image)

        # 损伤分类和责任判定
        damage_analysis = classify_damage(detections)

        # 获取车辆ID和订单ID
        vehicle_id = request.form.get('vehicle_id', '')
        order_id = request.form.get('order_id', '')

        return jsonify({
            'code': 200,
            'message': '检测完成',
            'data': {
                'vehicle_id': vehicle_id,
                'order_id': order_id,
                'detection_id': f"DET_{timestamp}",
                'image_path': f"/api/results/{result_filename}",
                'original_image': f"/api/uploads/{filename}",
                'drone_count': 1,
                'detections': detections,
                'damage_analysis': damage_analysis,
                'inference_time': round(inference_time, 3),
                'model_name': 'yolov8_drone_damage',
                'timestamp': timestamp
            }
        })

    except Exception as e:
        print(f"检测失败: {e}")
        return jsonify({'code': 500, 'message': f'检测失败: {str(e)}'}), 500


@app.route('/api/detect/video', methods=['POST'])
def detect_video():
    """
    视频损伤检测接口

    请求参数:
        - file: 上传的视频文件
        - vehicle_id: 无人机ID (可选)
        - order_id: 订单ID (可选)

    返回:
        JSON 格式的检测结果摘要
    """
    if 'file' not in request.files:
        return jsonify({'code': 400, 'message': '没有上传文件'}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({'code': 400, 'message': '未选择文件'}), 400

    if not allowed_file(file.filename):
        return jsonify({'code': 400, 'message': '不支持的文件格式'}), 400

    try:
        # 保存上传的视频
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        filename = secure_filename(f"{timestamp}_{file.filename}")
        filepath = UPLOAD_FOLDER / filename
        file.save(filepath)

        # 视频处理（处理前几秒进行快速检测）
        video = cv2.VideoCapture(str(filepath))

        fps = video.get(cv2.CAP_PROP_FPS)
        frame_count = int(video.get(cv2.CAP_PROP_FRAME_COUNT))

        # 处理最多60帧（约2-3秒）
        max_frames = min(60, frame_count)
        frame_interval = max(1, frame_count // max_frames)

        all_detections = []
        processed_frames = 0

        model = get_model()

        while processed_frames < max_frames:
            ret, frame = video.read()
            if not ret:
                break

            if processed_frames % frame_interval == 0:
                results = model(frame, conf=0.25, iou=0.45)
                if len(results) > 0 and results[0].boxes is not None:
                    boxes = results[0].boxes
                    for box in boxes:
                        cls = int(box.cls[0])
                        conf = float(box.conf[0])
                        all_detections.append({
                            'class': cls,
                            'confidence': conf,
                            'frame': processed_frames
                        })

            processed_frames += 1

        video.release()

        # 损伤分析
        damage_analysis = classify_damage(all_detections)

        return jsonify({
            'code': 200,
            'message': '视频检测完成',
            'data': {
                'vehicle_id': request.form.get('vehicle_id', ''),
                'order_id': request.form.get('order_id', ''),
                'detection_id': f"VIDEO_DET_{timestamp}",
                'video_path': f"/api/uploads/{filename}",
                'total_frames': frame_count,
                'processed_frames': processed_frames,
                'detections': all_detections,
                'damage_analysis': damage_analysis,
                'model_name': 'yolov8_drone_damage',
                'timestamp': timestamp
            }
        })

    except Exception as e:
        print(f"视频检测失败: {e}")
        return jsonify({'code': 500, 'message': f'视频检测失败: {str(e)}'}), 500


@app.route('/detection-api/uploads/<filename>')
def uploaded_file(filename):
    """获取上传的文件"""
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


@app.route('/detection-api/results/<filename>')
def result_file(filename):
    """获取结果文件"""
    return send_from_directory(RESULT_FOLDER, filename)


if __name__ == '__main__':
    print("=" * 50)
    print("无人机损伤检测 API 服务")
    print("=" * 50)
    print(f"上传目录: {UPLOAD_FOLDER}")
    print(f"结果目录: {RESULT_FOLDER}")
    print(f"模型路径: {MODEL_PATH}")
    print(f"模型存在: {MODEL_PATH.exists()}")
    print("=" * 50)

    # 预加载模型
    try:
        get_model()
        print("模型预加载成功！")
    except Exception as e:
        print(f"模型预加载失败: {e}")
        print("将在首次请求时加载模型")

    print("=" * 50)
    print("服务启动中...")
    print("API 地址: http://localhost:5000")
    print("健康检查: http://localhost:5000/api/health")
    print("=" * 50)

    app.run(host='0.0.0.0', port=5000, debug=True)
