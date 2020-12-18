# DragZoomRotateController
拖拽 放大缩小 旋转控件 可自己实现这些功能，也可添加子View 让子View实现这些功能 (但请注意：子View实现这些功能，只针对只有一个子View的时候） 拖动超过边界，拖动超边界回弹，拖动到边界不可继续拖动 放大缩小，双击放大，放大超出边界回弹

## 截图
![images](https://github.com/Wiser-Wong/DragZoomRotateController/blob/master/images/zoom.gif)

# 使用方法
    可动态配置属性参数，或者动画参数等等
    ImageView是拖拽放大缩小旋转的主角及有子View的时候操作的是子View
        <com.wiser.dragzoomrotatelayout.DragZoomRotateController
                android:layout_margin="20dp"
                android:id="@+id/dzr_controller"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="@drawable/zoom_bg"
                app:dzr_isLimitSpringAnim="true"
                app:dzr_isRotate="true"
                app:dzr_isDrag="true"
                app:dzr_isScale="true"
                app:dzr_isDoubleScale="true"
                app:dzr_isDragLimit="false"
                app:dzr_minScale="0.5"
                app:dzr_midScale="2.5"
                app:dzr_maxScale="4"
                app:layout_constraintBottom_toBottomOf="parent"
                app:layout_constraintLeft_toLeftOf="parent"
                app:layout_constraintRight_toRightOf="parent"
                app:layout_constraintTop_toTopOf="parent" >
        
                <androidx.appcompat.widget.AppCompatImageView
                    android:layout_width="100dp"
                    android:layout_height="100dp"
                    android:layout_gravity="center"
                    android:adjustViewBounds="true"
                    android:src="@drawable/ip"/>
        
        </com.wiser.dragzoomrotatelayout.DragZoomRotateController>
    背景是拖拽放大缩小的主角及没有子View的时候，操作的是自己
    <com.wiser.dragzoomrotatelayout.DragZoomRotateController
                    android:layout_margin="20dp"
                    android:id="@+id/dzr_controller"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:background="@drawable/zoom_bg"
                    app:dzr_isLimitSpringAnim="true"
                    app:dzr_isRotate="true"
                    app:dzr_isDrag="true"
                    app:dzr_isScale="true"
                    app:dzr_isDoubleScale="true"
                    app:dzr_isDragLimit="false"
                    app:dzr_minScale="0.5"
                    app:dzr_midScale="2.5"
                    app:dzr_maxScale="4"
                    app:layout_constraintBottom_toBottomOf="parent"
                    app:layout_constraintLeft_toLeftOf="parent"
                    app:layout_constraintRight_toRightOf="parent"
                    app:layout_constraintTop_toTopOf="parent" >
            
            </com.wiser.dragzoomrotatelayout.DragZoomRotateController>