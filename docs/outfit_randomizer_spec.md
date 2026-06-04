# 隨機穿搭產生器 — Android App 設計規格

> 本文件為交付 Claude Code 的開發規格。內容已釐清所有功能定義，可直接作為實作依據。

---

## 1. 產品概述

一款本機（offline）Android App，協助使用者隨機決定「今天要穿什麼」。使用者建立自己的衣櫃（衣物清單），每件衣物可歸屬於一個或多個「部位」。產生穿搭時，使用者勾選想要的部位，App 為每個部位隨機抽出一件「可用」的衣物。可針對單一部位或全部重新隨機（例如某件衣服拿去洗了）。

- **平台**：Android
- **資料儲存**：純本機（Room / SQLite），無後端、無網路需求
- **核心特性**：兩層結構（部位 → 衣物）、多對多歸屬、隨機產生、單／全部重骰、送洗狀態

---

## 2. 已釐清的需求決策

下列決策已與需求方確認，實作時直接遵循，無需再行假設。

| # | 議題 | 決策 |
|---|------|------|
| 1 | 衣物與部位的歸屬關係 | **多對多**。一件衣物可同時屬於多個部位（例：連帽外套同屬「外套」與「帽子」） |
| 2 | 同一部位每次抽幾件 | **固定一件** |
| 3 | 重骰範圍 | **單一部位重骰** 與 **全部重骰** 兩者都要 |
| 4 | 跨部位衣物是否可在同一套出現兩次 | **可重複**。每部位獨立抽取，互不影響，不做全局去重 |
| 5 | 是否儲存／收藏產生的穿搭 | **不儲存**，用完即丟 |
| 6 | 衣物記錄的詳細度 | 名稱 + **季節標籤**（季節僅作標籤儲存，本期不參與篩選邏輯，欄位保留供未來擴充） |
| 7 | 「送洗中」如何恢復 | **手動切回**可用（不做自動恢復或提醒） |
| 8 | 某部位無任何可用衣物時 | 該部位顯示「**無可用衣物**」，其餘部位照常產生 |
| 9 | 衣櫃資料規模 | **大（>200 件）**，衣物管理畫面需具備**搜尋**與**分頁** |

---

## 3. 資料模型（Room）

### 3.1 Entity 定義

```kotlin
@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0          // 用於部位排序（拖曳）
)

@Entity(indices = [Index(value = ["name"])])   // name 建索引，加速搜尋
data class ClothingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                // 例：Uniqlo Airism 藍色上衣
    val isAvailable: Boolean = true, // 手動切換；送洗中 = false
    val seasons: Int = 0             // 季節 bit flag（純標籤，本期不篩選）
)

// 多對多中間表：一件衣物 ↔ 多個部位
@Entity(
    primaryKeys = ["itemId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = ClothingItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class ItemCategoryCrossRef(
    val itemId: Long,
    val categoryId: Long
)
```

### 3.2 季節 Bit Flag 定義

以單一 `Int` 存放多選季節，避免額外資料表。

```kotlin
object Season {
    const val SPRING = 1   // 0001
    const val SUMMER = 2   // 0010
    const val AUTUMN = 4   // 0100
    const val WINTER = 8   // 1000
    const val ALL    = SPRING or SUMMER or AUTUMN or WINTER  // 15

    // 判斷某衣物是否含特定季節
    fun has(value: Int, season: Int) = (value and season) != 0
}
```

> 本期季節僅在衣物編輯畫面以多選 chip 設定並儲存、列表可顯示，但**不參與隨機產生的篩選**。保留欄位是為了日後加入「只抽夏天的衣服」這類篩選時無需做資料遷移。

### 3.3 關聯查詢用的 POJO

```kotlin
// 一件衣物連同其所屬部位（供衣物管理列表/編輯顯示）
data class ItemWithCategories(
    @Embedded val item: ClothingItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemCategoryCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
)
```

---

## 4. DAO 介面（建議簽名）

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<Category>>

    @Insert suspend fun insert(category: Category): Long
    @Update suspend fun update(category: Category)
    @Delete suspend fun delete(category: Category)   // CASCADE 自動清除關聯

    @Query("UPDATE Category SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)
}

@Dao
interface ClothingItemDao {
    // 衣物管理列表：搜尋 + 分頁（>200 件）
    @Transaction
    @Query("""
        SELECT * FROM ClothingItem
        WHERE name LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun pagingSearch(query: String): PagingSource<Int, ItemWithCategories>

    // 隨機產生用：取某部位「可用」衣物
    @Query("""
        SELECT ci.* FROM ClothingItem ci
        INNER JOIN ItemCategoryCrossRef ref ON ci.id = ref.itemId
        WHERE ref.categoryId = :categoryId AND ci.isAvailable = 1
    """)
    suspend fun getAvailableItemsByCategory(categoryId: Long): List<ClothingItem>

    @Transaction
    @Query("SELECT * FROM ClothingItem WHERE id = :id")
    suspend fun getItemWithCategories(id: Long): ItemWithCategories?

    @Insert suspend fun insertItem(item: ClothingItem): Long
    @Update suspend fun updateItem(item: ClothingItem)
    @Delete suspend fun deleteItem(item: ClothingItem)

    @Query("UPDATE ClothingItem SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailable(id: Long, available: Boolean)
}

@Dao
interface CrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ItemCategoryCrossRef)

    @Query("DELETE FROM ItemCategoryCrossRef WHERE itemId = :itemId")
    suspend fun clearItemRefs(itemId: Long)

    // 編輯衣物時：先清空再重建該衣物的部位關聯
    @Transaction
    suspend fun setItemCategories(itemId: Long, categoryIds: List<Long>) {
        clearItemRefs(itemId)
        categoryIds.forEach { insert(ItemCategoryCrossRef(itemId, it)) }
    }
}
```

---

## 5. 核心邏輯（隨機產生）

每個部位**獨立**抽取，互不影響，**不做全局去重**（決策 #4）。空清單回傳 `null`，由 UI 顯示「無可用衣物」（決策 #8）。

```kotlin
class OutfitGenerator(
    private val itemDao: ClothingItemDao,
    private val categoryDao: CategoryDao
) {
    /** 產生整套：每個勾選部位抽一件可用衣物 */
    suspend fun generate(selectedCategoryIds: List<Long>): List<OutfitSlot> =
        selectedCategoryIds.map { catId ->
            val category = categoryDao.getById(catId)
            val picked = itemDao.getAvailableItemsByCategory(catId).randomOrNull()
            OutfitSlot(category = category, item = picked)  // item 可為 null
        }

    /** 單部位重骰：盡量換成不同的一件；只剩一件時維持原狀 */
    suspend fun rerollSingle(categoryId: Long, currentItemId: Long?): ClothingItem? {
        val pool = itemDao.getAvailableItemsByCategory(categoryId)
        return pool.filterNot { it.id == currentItemId }.randomOrNull()
            ?: pool.randomOrNull()   // pool 為空則回 null
    }

    /** 全部重骰：等同以目前勾選部位重新 generate() */
}

data class OutfitSlot(
    val category: Category,
    val item: ClothingItem?   // null = 該部位無可用衣物
)
```

---

## 6. 畫面規格

### 6.1 產生畫面（Generate Screen）

- 顯示所有部位的多選清單（Checkbox 或 FilterChip）。
- 記住上次勾選狀態（存於 `DataStore` 或 ViewModel + savedState）。
- 主行動按鈕「產生穿搭」→ 導向結果畫面（或同頁下方展開結果）。

### 6.2 結果畫面（Result Screen）

每個被勾選的部位顯示一列：

```
帽子    黑色棒球帽              🔄
上衣    Uniqlo Airism 藍色      🔄
外套    無可用衣物              🔄   ← 灰字
內褲    灰色四角褲              🔄

            [ 全部重骰 ]
```

- 每列右側 🔄：呼叫 `rerollSingle`，僅更新該列。
- 空部位：顯示灰字「無可用衣物」，🔄 仍可用（若稍後將衣物切回可用即可救回）。
- 底部固定「全部重骰」：以目前勾選部位重新 `generate`。

### 6.3 衣物管理畫面（Item Management Screen）

- 頂部 `SearchBar`：依名稱即時過濾（對應 `pagingSearch`）。
- 使用 **Paging 3** 分頁載入，避免一次撈 200+ 筆。
- 列表每項：衣物名稱 + 所屬部位 chips + 送洗開關（Switch，切換 `isAvailable`）。
- 點入編輯／新增：
  - 名稱（TextField）
  - 所屬部位（多選 chip，可選多個）
  - 季節（多選 chip：春/夏/秋/冬）
  - 送洗狀態（Switch）
  - 儲存時透過 `CrossRefDao.setItemCategories` 重建關聯。

### 6.4 部位管理畫面（Category Management Screen）

- 新增 / 改名 / 刪除 / 拖曳排序（更新 `sortOrder`）。
- 刪除部位提示：將解除其與衣物的關聯，但**不會刪除衣物本身**（CASCADE 僅刪中間表）。

---

## 7. 技術選型

| 項目 | 選擇 |
|------|------|
| UI | Jetpack Compose |
| 架構 | MVVM + Repository |
| 資料庫 | Room（多對多以 `@Relation` + `Junction` 查詢） |
| 列表分頁 | Paging 3（衣物管理列表，因 >200 件） |
| 狀態管理 | ViewModel + StateFlow |
| 依賴注入 | Hilt（多畫面 + 資料層，建議導入） |
| 設定儲存 | DataStore（記住上次勾選的部位） |
| 後端 | 無（純本機） |

---

## 8. 建議開發順序

1. **資料層**：Room schema、Entity、DAO（含多對多查詢與 `ItemWithCategories`），寫基本 DAO 測試。
2. **管理畫面**：衣物 / 部位的 CRUD（先能建立資料才能測試其他功能）。
3. **核心功能**：產生畫面 + 結果畫面（`generate` / `rerollSingle` / 全部重骰）。
4. **規模化**：衣物列表搜尋 + Paging 3 分頁、送洗開關。
5. **打磨**：部位拖曳排序、記住勾選狀態、空狀態與錯誤處理、UI 細節。

---

## 9. 未來擴充（本期不做，但已預留）

- **季節篩選**：`seasons` 欄位已存在，未來可在產生畫面加季節選擇器，於 `getAvailableItemsByCategory` 加上 `(seasons & :filter) != 0` 條件。
- **穿搭收藏 / 每日紀錄**：目前用完即丟；未來可加 `Outfit` 與 `OutfitItem` 表保存歷史。
- **衣物照片 / 顏色 / 品牌**：可於 `ClothingItem` 增欄位，不影響既有結構。
- **送洗自動恢復 / 提醒**：可於送洗時記錄日期，加入排程提醒。

---

## 10. 邊界與規則備忘（給實作者）

- 跨部位衣物在同一套搭配中**可能重複出現**，這是預期行為，非 bug。
- 隨機抽取只考慮 `isAvailable = true` 的衣物。
- 刪除部位 / 刪除衣物均透過 FOREIGN KEY `CASCADE` 自動清理中間表，務必在 Room 啟用 `foreignKeys` 並開啟外鍵約束。
- 衣物可以不屬於任何部位（孤兒衣物）；此時它不會出現在任何隨機產生中，但仍可在衣物管理中被搜尋到。
