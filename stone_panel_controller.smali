.class public Lcom/android/systemui/stone/StonePanelController;
.super Ljava/lang/Object;
.source "StonePanelController.java"

# static fields
.field private static final TAG:Ljava/lang/String; = "StoneOS"

# instance fields
.field private mContext:Landroid/content/Context;
.field private mPanelView:Landroid/view/View;
.field private mWebView:Landroid/webkit/WebView;
.field private mWindowManager:Landroid/view/WindowManager;
.field private mIsShowing:Z

# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    
    iput-object p1, p0, Lcom/android/systemui/stone/StonePanelController;->mContext:Landroid/content/Context;
    
    const/4 v0, 0x0
    iput-boolean v0, p0, Lcom/android/systemui/stone/StonePanelController;->mIsShowing:Z
    
    invoke-direct {p0}, Lcom/android/systemui/stone/StonePanelController;->init()V
    
    return-void
.end method

.method private init()V
    .locals 5

    # Log initialization
    const-string v0, "StoneOS"
    const-string v1, "Initializing Stone Panel Controller"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    # Get window manager
    iget-object v0, p0, Lcom/android/systemui/stone/StonePanelController;->mContext:Landroid/content/Context;
    const-string v1, "window"
    invoke-virtual {v0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/view/WindowManager;
    iput-object v0, p0, Lcom/android/systemui/stone/StonePanelController;->mWindowManager:Landroid/view/WindowManager;

    # Create panel view
    invoke-direct {p0}, Lcom/android/systemui/stone/StonePanelController;->createPanelView()V

    return-void
.end method

.method private createPanelView()V
    .locals 4

    # Create a simple TextView as Stone icon for now
    new-instance v0, Landroid/widget/TextView;
    iget-object v1, p0, Lcom/android/systemui/stone/StonePanelController;->mContext:Landroid/content/Context;
    invoke-direct {v0, v1}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    
    const-string v1, "🗿"
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    
    const/high16 v1, 0x41f00000    # 30.0f
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextSize(F)V
    
    const/16 v1, 0x11  # Gravity.CENTER
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setGravity(I)V
    
    const/4 v1, -0x1  # White background
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setBackgroundColor(I)V
    
    iput-object v0, p0, Lcom/android/systemui/stone/StonePanelController;->mPanelView:Landroid/view/View;

    return-void
.end method

.method public showStoneIcon()V
    .locals 5

    iget-boolean v0, p0, Lcom/android/systemui/stone/StonePanelController;->mIsShowing:Z
    if-eqz v0, :cond_0
    return-void
    
    :cond_0
    const-string v0, "StoneOS"
    const-string v1, "Showing Stone Icon"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    # Create layout params for overlay
    new-instance v0, Landroid/view/WindowManager$LayoutParams;
    const/16 v1, 0x64  # width 100px
    const/16 v2, 0x64  # height 100px
    const/16 v3, 0x7d6  # TYPE_SYSTEM_ERROR for system overlay
    const/16 v4, 0x8   # FLAG_NOT_FOCUSABLE
    const/4 v5, -0x3  # PixelFormat.TRANSLUCENT
    invoke-direct {v0, v1, v2, v3, v4, v5}, Landroid/view/WindowManager$LayoutParams;-><init>(IIIII)V

    # Position at bottom center
    const/16 v1, 0x51  # Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
    iput v1, v0, Landroid/view/WindowManager$LayoutParams;->gravity:I

    # Add view to window
    iget-object v1, p0, Lcom/android/systemui/stone/StonePanelController;->mWindowManager:Landroid/view/WindowManager;
    iget-object v2, p0, Lcom/android/systemui/stone/StonePanelController;->mPanelView:Landroid/view/View;
    invoke-interface {v1, v2, v0}, Landroid/view/WindowManager;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    const/4 v0, 0x1
    iput-boolean v0, p0, Lcom/android/systemui/stone/StonePanelController;->mIsShowing:Z

    return-void
.end method