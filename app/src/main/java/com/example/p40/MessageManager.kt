package com.example.p40

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Queue

/**
 * 게임 내 메시지를 표시하고 관리하는 클래스
 * Toast 대신 화면 상단에 메시지를 표시하고, 메시지 큐를 관리합니다.
 */
class MessageManager private constructor() {
    companion object {
        // 싱글톤 패턴 구현
        @Volatile
        private var instance: MessageManager? = null

        fun getInstance(): MessageManager {
            return instance ?: synchronized(this) {
                instance ?: MessageManager().also { instance = it }
            }
        }
    }

    // 메시지 큐 및 현재 표시된 메시지 수 관리
    private val messageQueue: Queue<MessageInfo> = LinkedList()
    private var currentMessageCount = 0
    private var containerViewRef: WeakReference<ViewGroup>? = null
    private val handler = Handler(Looper.getMainLooper())

    // 메시지 정보 데이터 클래스
    data class MessageInfo(
        val message: String,
        val type: MessageType = MessageType.INFO
    )

    // 메시지 타입 (색상 구분을 위함)
    enum class MessageType {
        INFO,      // 일반 정보 (파란색)
        SUCCESS,   // 성공 메시지 (녹색)
        WARNING,   // 경고 메시지 (주황색)
        ERROR      // 오류 메시지 (빨간색)
    }

    /**
     * 메시지 컨테이너 초기화
     * Activity/Fragment의 root view에 메시지 컨테이너 추가
     */
    fun init(rootView: ViewGroup) {
        // 이미 초기화되었으면 반환
        if (containerViewRef?.get() != null) return

        // 메시지 컨테이너 생성
        val newContainerView = LinearLayout(rootView.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(rootView.context, 24) // 상단 여백 증가
            }
            // 컨테이너에 패딩 설정
            setPadding(dpToPx(rootView.context, 16), dpToPx(rootView.context, 8), dpToPx(rootView.context, 16), 0)
        }
        
        // WeakReference로 저장
        containerViewRef = WeakReference(newContainerView)

        // Fragment 환경을 고려한 안전한 View 추가 방법
        try {
            // 최상위 Activity의 content view를 찾아 거기에 추가
            val activity = getActivity(rootView.context)
            if (activity != null) {
                val decorView = activity.window.decorView as ViewGroup
                val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)
                contentView.addView(newContainerView)
            } else {
                // 활동을 찾을 수 없는 경우 - 기본 동작으로 폴백
                fallbackAddView(rootView, newContainerView)
            }
        } catch (e: Exception) {
            // 예외 발생 시 기본 동작으로 폴백
            fallbackAddView(rootView, newContainerView)
        }
    }

    /**
     * 기본 View 추가 방법 (대체 방식)
     */
    private fun fallbackAddView(rootView: ViewGroup, containerView: ViewGroup) {
        try {
            // FrameLayout만 직접 추가 시도
            if (rootView is FrameLayout && rootView !is androidx.fragment.app.FragmentContainerView) {
                rootView.addView(containerView)
            } else {
                // 다른 타입의 ViewGroup은 Toast로 대체
                showToast("UI 초기화에 실패했습니다. 일부 기능이 제한될 수 있습니다.")
            }
        } catch (e: Exception) {
            // 최종 실패 시 아무 작업도 하지 않음
            showToast("UI 초기화에 실패했습니다. 일부 기능이 제한될 수 있습니다.")
        }
    }

    /**
     * Context에서 Activity 찾기
     */
    private fun getActivity(context: Context): android.app.Activity? {
        if (context is android.app.Activity) {
            return context
        } else if (context is android.content.ContextWrapper) {
            return getActivity(context.baseContext)
        }
        return null
    }

    /**
     * Toast 대체 메시지 (실패 시)
     */
    private fun showToast(message: String) {
        handler.post {
            android.widget.Toast.makeText(
                containerViewRef?.get()?.context,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 새 메시지 추가
     */
    fun showMessage(message: String, type: MessageType = MessageType.INFO) {
        handler.post {
            messageQueue.offer(MessageInfo(message, type))
            processMessageQueue()
        }
    }

    /**
     * 정보 메시지 표시 (파란색)
     */
    fun showInfo(message: String) {
        showMessage(message, MessageType.INFO)
    }

    /**
     * Context를 받는 정보 메시지 표시 (파란색)
     */
    fun showInfo(context: Context, message: String) {
        initIfNeeded(context)
        showInfo(message)
    }

    /**
     * 성공 메시지 표시 (녹색)
     */
    fun showSuccess(message: String) {
        showMessage(message, MessageType.SUCCESS)
    }

    /**
     * Context를 받는 성공 메시지 표시 (녹색)
     */
    fun showSuccess(context: Context, message: String) {
        initIfNeeded(context)
        showSuccess(message)
    }

    /**
     * 경고 메시지 표시 (주황색)
     */
    fun showWarning(message: String) {
        showMessage(message, MessageType.WARNING)
    }

    /**
     * Context를 받는 경고 메시지 표시 (주황색)
     */
    fun showWarning(context: Context, message: String) {
        initIfNeeded(context)
        showWarning(message)
    }

    /**
     * 오류 메시지 표시 (빨간색)
     */
    fun showError(message: String) {
        showMessage(message, MessageType.ERROR)
    }

    /**
     * Context를 받는 오류 메시지 표시 (빨간색)
     */
    fun showError(context: Context, message: String) {
        initIfNeeded(context)
        showError(message)
    }

    /**
     * Context가 있을 때 필요하면 초기화
     */
    private fun initIfNeeded(context: Context) {
        if (containerViewRef?.get() == null) {
            // Activity의 content view를 찾아 초기화
            val activity = getActivity(context)
            if (activity != null) {
                init(activity.findViewById(android.R.id.content))
            } else {
                // 활동을 찾을 수 없는 경우 - Toast로 대체
                android.widget.Toast.makeText(
                    context,
                    "메시지 시스템 초기화에 실패했습니다",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Context를 받는 Toast 대체 메시지
     */
    private fun showToast(context: Context, message: String) {
        handler.post {
            android.widget.Toast.makeText(
                context,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 메시지 큐 처리
     */
    private fun processMessageQueue() {
        val container = containerViewRef?.get() ?: return

        // 최대 메시지 수를 초과하지 않고, 큐에 메시지가 있는 경우에만 처리
        if (currentMessageCount < GameConfig.MESSAGE_MAX_COUNT && messageQueue.isNotEmpty()) {
            val messageInfo = messageQueue.poll() ?: return
            
            // 메시지 뷰 생성
            val messageView = createMessageView(container.context, messageInfo)
            
            // 컨테이너에 메시지 뷰 추가
            container.addView(messageView, 0) // 새 메시지는 항상 맨 위에 표시
            
            // 메시지 카운트 증가
            currentMessageCount++
            
            // 메시지 등장 애니메이션
            showMessageWithAnimation(messageView)
            
            // MESSAGE_DURATION 후 메시지 제거
            handler.postDelayed({
                hideMessageWithAnimation(messageView)
            }, GameConfig.MESSAGE_DURATION)
        }
    }

    /**
     * 메시지 뷰 생성
     */
    private fun createMessageView(context: Context, messageInfo: MessageInfo): View {
        // 메시지 레이아웃 inflate 또는 동적 생성
        val messageView = TextView(context)
        
        // 텍스트 및 스타일 설정
        messageView.apply {
            text = messageInfo.message
            textSize = GameConfig.MESSAGE_TEXT_SIZE // GameConfig에서 설정한, 텍스트 크기 사용
            setTextColor(Color.WHITE)
            // 텍스트에 그림자 추가하여 가독성 향상
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
            // 패딩 설정
            setPadding(
                dpToPx(context, GameConfig.MESSAGE_PADDING_HORIZONTAL),
                dpToPx(context, GameConfig.MESSAGE_PADDING_VERTICAL),
                dpToPx(context, GameConfig.MESSAGE_PADDING_HORIZONTAL),
                dpToPx(context, GameConfig.MESSAGE_PADDING_VERTICAL)
            )
            gravity = Gravity.CENTER
            
            // 너비 제약조건 설정
            minimumWidth = dpToPx(context, GameConfig.MESSAGE_MIN_WIDTH)
            maxWidth = dpToPx(context, GameConfig.MESSAGE_MAX_WIDTH)
            
            // 초기에는 보이지 않게 설정
            alpha = 0f
        }
        
        // 배경 색상 설정 (알파값 추가)
        val baseColor = when (messageInfo.type) {
            MessageType.INFO -> Color.parseColor("#3498db")     // 파란색
            MessageType.SUCCESS -> Color.parseColor("#2ecc71")  // 녹색
            MessageType.WARNING -> Color.parseColor("#f39c12")  // 주황색
            MessageType.ERROR -> Color.parseColor("#e74c3c")    // 빨간색
        }
        
        // 알파값 적용 (GameConfig에서 설정한 불투명도)
        val alpha = (255 * GameConfig.MESSAGE_OPACITY).toInt()
        val backgroundColor = Color.argb(
            alpha,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
        
        // 테두리 색상 설정
        val strokeColor = when (messageInfo.type) {
            MessageType.INFO -> Color.parseColor("#2980b9")     // 어두운 파란색
            MessageType.SUCCESS -> Color.parseColor("#27ae60")  // 어두운 녹색
            MessageType.WARNING -> Color.parseColor("#d35400")  // 어두운 주황색
            MessageType.ERROR -> Color.parseColor("#c0392b")    // 어두운 빨간색
        }
        
        // 배경 드로어블 설정
        messageView.background = getBackgroundDrawable(backgroundColor, strokeColor)
        
        // 레이아웃 파라미터 설정
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 레이아웃 파라미터 속성 설정
        layoutParams.apply { 
            gravity = Gravity.CENTER_HORIZONTAL
            setMargins(0, dpToPx(context, 4), 0, dpToPx(context, 4))
        }
        
        // 뷰에 레이아웃 파라미터 할당
        messageView.layoutParams = layoutParams
        
        return messageView
    }

    /**
     * 배경 드로어블 생성 (테두리 포함)
     */
    private fun getBackgroundDrawable(fillColor: Int, strokeColor: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(fillColor)
            setStroke(dpToPx(null, 1), strokeColor)
            cornerRadius = dpToPx(null, GameConfig.MESSAGE_CORNER_RADIUS).toFloat()
        }
    }

    /**
     * 메시지 표시 애니메이션
     */
    private fun showMessageWithAnimation(view: View) {
        // 초기 위치 설정 (위에서 아래로 슬라이드)
        view.translationY = -50f
        view.alpha = 0f
        
        // 애니메이션 시작
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    /**
     * 메시지 숨김 애니메이션
     */
    private fun hideMessageWithAnimation(view: View) {
        view.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // 애니메이션 종료 후 뷰 제거
                    val parent = view.parent as? ViewGroup
                    parent?.removeView(view)
                    
                    // 메시지 카운트 감소
                    currentMessageCount--
                    
                    // 큐에 대기 중인 메시지가 있으면 처리
                    processMessageQueue()
                }
            })
            .start()
    }

    /**
     * dp를 px로 변환
     */
    private fun dpToPx(context: Context?, dp: Int): Int {
        val density = context?.resources?.displayMetrics?.density ?: containerViewRef?.get()?.context?.resources?.displayMetrics?.density ?: 2.0f
        return (dp * density).toInt()
    }

    /**
     * 모든 메시지 제거 및 대기 중인 작업 취소 (게임 종료 시 사용)
     */
    fun clear() {
        // 메시지 큐 비우기
        messageQueue.clear()
        
        // 핸들러의 모든 콜백 제거
        handler.removeCallbacksAndMessages(null)
        
        // 현재 표시 중인 모든 메시지 제거
        containerViewRef?.get()?.let { container ->
            container.removeAllViews()
        }
        
        // 카운터 초기화
        currentMessageCount = 0
        
        // 컨테이너 뷰 참조 해제
        containerViewRef = null
    }
} 