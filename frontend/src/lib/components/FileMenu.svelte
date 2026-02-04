<script lang="ts">
  import { enhance } from '$app/forms';
  import { goto } from '$app/navigation';
  import type { UserResponse } from '$lib/types/auth';
  import { onMount } from 'svelte';
//   import { scheduleIntervals } from '$lib/stores/scheduleStore';
  
  let isOpen = $state(false);
  let isSubMenuOpen = $state(false);
  let menuRef = $state<HTMLDivElement | null>(null);

  let {
    userData,
    onOperatorCreate,
    onReferenceCreate,
    intervals = [],
    operatorData = null,
    ppiAssignments = [],
    selectedProgramDate = '',
    createdPrograms = []
  } = $props<{
    userData: UserResponse | null;
    onOperatorCreate?: () => void;
    onReferenceCreate?: () => void;
    intervals?: TimeInterval[];
    operatorData?: any;
    ppiAssignments?: any[];
    selectedProgramDate?: string;
    createdPrograms?: any[];
  }>();

  function handleClickOutside(event: MouseEvent) {
    if (menuRef && !menuRef.contains(event.target as Node)) {
      isOpen = false;
      isSubMenuOpen = false;
    }
  }

  onMount(() => {
    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  });

  function handleCreateSchedule() {
    isSubMenuOpen = true;
  }

  function handleCreateByOperator() {
    if (onOperatorCreate) {
      onOperatorCreate();
    }

    isSubMenuOpen = false;
    isOpen = false;
  }

  function handleCreateByReference() {
    if (onReferenceCreate) {
      onReferenceCreate();
    }

    isSubMenuOpen = false;
    isOpen = false;
  }

  async function handleSave() {
    console.log("=== СОХРАНЕНИЕ ПРЦА ===");
    
    // 1. Логируем текущее состояние
    console.log("Intervals (количество):", intervals.length);
    console.log("Intervals (детали):", intervals);
    
    console.log("Operator Data:", operatorData);
    console.log("PPI Assignments:", ppiAssignments);
    console.log("Selected Date:", selectedProgramDate);
    console.log("Created Programs:", createdPrograms);
    
    // 2. Проверяем данные
    if (!operatorData || !selectedProgramDate) {
      console.error("Недостаточно данных для сохранения!");
      alert('Ошибка: Нет данных оператора или дата не выбрана');
      return;
    }
    
    if (intervals.length === 0 && (!operatorData.kvdList && !operatorData.tnpList && !operatorData.tsList)) {
      console.error("Нет интервалов для сохранения!");
      alert('Ошибка: Нет интервалов для сохранения');
      return;
    }
    
    // 3. Импортируем сервис для подготовки данных
    try {
      const { ScheduleCreationService } = await import('../../features/services/scheduleCreation.service');
      
      console.log("=== ПОДГОТОВКА ДАННЫХ ДЛЯ СОХРАНЕНИЯ ===");
      
      // Подготавливаем данные программы
      const programRequest = ScheduleCreationService.prepareFullProgramData(
        operatorData,
        ppiAssignments,
        createdPrograms,
        selectedProgramDate,
        "00:00", // Время по умолчанию
        'main'   // Статус по умолчанию
      );
      
      // 4. ЛОГИРУЕМ ВСЕ ДАННЫЕ, КОТОРЫЕ БУДУТ ОТПРАВЛЕНЫ
      console.log("=== СОЗДАННЫЙ ЗАПРОС НА СОХРАНЕНИЕ ===");
      console.log("Полный запрос (JSON):", JSON.stringify(programRequest, null, 2));
      
      console.log("\n=== ДЕТАЛЬНЫЙ АНАЛИЗ ===");
      console.log("Основные данные программы:");
      console.log("- Номер ПРЦА:", programRequest.mainData.numRp);
      console.log("- Номер КА:", programRequest.mainData.numKa);
      console.log("- Дата начала:", programRequest.mainData.dateOn);
      console.log("- Дата окончания:", programRequest.mainData.dateOff);
      console.log("- Тип ПРЦА:", programRequest.mainData.typeRp);
      
      console.log("\nРежимы работы (" + programRequest.modes.length + " шт.):");
      programRequest.modes.forEach((mode, index) => {
        console.log(`\n--- Режим ${index + 1} ---`);
        console.log("- Код режима:", mode.kodMode);
        console.log("- Дата начала:", mode.dateOn);
        console.log("- Дата окончания:", mode.dateOff);
        console.log("- Номер ППИ:", mode.numPpi);
        console.log("- Длительность:", mode.dlit, "сек");
        console.log("- Заказчик:", mode.zakazchik || "не указан");
        
        if (mode.kvdData) {
          console.log("- Тип: КВД");
          console.log("  Время КВД:", mode.kvdData.dn, "-", mode.kvdData.dk);
          console.log("  МСУ:", mode.kvdData.prMsu === 0 ? "МСУ-1" : "МСУ-2");
          console.log("  БССД:", mode.kvdData.prBssd === 0 ? "БССД1" : "БССД2");
          console.log("  ЗГ:", mode.kvdData.prZg);
        }
        
        if (mode.tsData) {
          console.log("- Тип: ТС");
          console.log("  Время ТС:", mode.tsData.dn, "-", mode.tsData.dk);
          console.log("  Тип съемки:", mode.tsData.tip);
          console.log("  Режим:", mode.tsData.reg);
        }
        
        if (mode.tnpData) {
          console.log("- Тип: ТНП");
          console.log("  Время ТНП:", mode.tnpData.dn, "-", mode.tnpData.dk);
          console.log("  Длительность:", mode.tnpData.dlit, "сек");
        }
      });
      
      console.log("\n=== СВОДКА ===");
      const kvdCount = programRequest.modes.filter(m => m.kodMode === 7).length;
      const tnpCount = programRequest.modes.filter(m => m.kodMode === 4).length;
      const tsCount = programRequest.modes.filter(m => m.kodMode === 8).length;
      
      console.log(`Всего режимов: ${programRequest.modes.length}`);
      console.log(`КВД: ${kvdCount}`);
      console.log(`ТНП: ${tnpCount}`);
      console.log(`ТС: ${tsCount}`);
      
      // 5. Проверка времени интервалов
      console.log("\n=== ПРОВЕРКА ВРЕМЕНИ ИНТЕРВАЛОВ ===");
      let hasCrossDayIntervals = false;
      programRequest.modes.forEach((mode, index) => {
        const startDate = new Date(mode.dateOn);
        const endDate = new Date(mode.dateOff);
        
        if (startDate.getDate() !== endDate.getDate()) {
          console.warn(`Режим ${index + 1} пересекает границу суток!`);
          console.warn(`  Начало: ${mode.dateOn}`);
          console.warn(`  Конец: ${mode.dateOff}`);
          hasCrossDayIntervals = true;
        }
      });
      
      if (hasCrossDayIntervals) {
        console.error("ВНИМАНИЕ: Найдены интервалы, пересекающие границу суток!");
      }
      
      // 6. Показываем сообщение
      alert(`Данные подготовлены к сохранению!\n\n` +
            `Всего режимов: ${programRequest.modes.length}\n` +
            `КВД: ${kvdCount}, ТНП: ${tnpCount}, ТС: ${tsCount}\n\n` +
            `Проверьте консоль браузера (F12 → Console) для деталей.`);
            
      console.log("=== КОНЕЦ ЛОГИРОВАНИЯ ===");
      
      // 7. Здесь будет реальное сохранение
      // const result = await ScheduleCreationService.saveProgram(programRequest);
      // console.log("Результат сохранения:", result);
      // alert('ПРЦА успешно сохранен!');
      
    } catch (error) {
      console.error("Ошибка при подготовке данных:", error);
      alert('Ошибка при подготовке данных для сохранения: ' + error.message);
    }
  }

  function handleExport() {
    alert('Генерация отчета');
  }

  function handleArchive() {
    goto('/archive');
  }

  function handleLogout() {
    if (confirm('Вы уверены, что хотите выйти?')) {
      goto('/');
    }
  }

  function handleAdmin() {
    goto('/admin');
  }

  function toggleMenu(event: MouseEvent) {
    event.stopPropagation();
    isOpen = !isOpen;
    if (!isOpen) {
      isSubMenuOpen = false;
    }
  }
</script>

<div class="file-menu" bind:this={menuRef}>
  <button class="menu-button" onclick={toggleMenu}>
    Действия
  </button>

  {#if isOpen}
    <div class="dropdown">
      <div class="dropdown-content">
        <div onclick={handleCreateSchedule} class="menu-item with-submenu">
          Создать ПРЦА
          {#if isSubMenuOpen}
            <div class="submenu">
              <button onclick="{handleCreateByOperator}" class="submenu-item">
                По данным оператора
              </button>
              <button onclick="{handleCreateByReference}" class="submenu-item">
                По опорной ПРЦА
              </button>
            </div>
          {/if}
        </div>
        <button onclick={handleSave} class="menu-item">
          Сохранить ПРЦА
        </button>
        <hr />
        <button onclick={handleExport} class="menu-item">
          Отчет
        </button>
        <button onclick={handleArchive} class="menu-item">
          Просмотр архива
        </button>
        <hr />
                
        <div class="user-info">
            {#if userData}
                <div class="user-details">
                    <div class="user-name">
                        {userData.firstName} {userData.lastName}
                    </div>
                    <div class="user-meta">
                        <span class="username">@{userData.username}</span>
                        {#if userData.roles && userData.roles.length > 0}
                            <span class="user-role">• {userData.roles[0]}</span>
                        {/if}
                    </div>
                </div>
            {:else}
                <div class="user-details">
                    <div class="user-name">
                        Не авторизован
                    </div>
                </div>
            {/if}
        </div>
        {#if userData?.roles?.includes('ADMIN')}
            <button onclick={handleAdmin} type="submit" class="menu-item">Панель админа</button>
        {/if}
        <form method="POST" action="/?/logout" use:enhance>
            <button onclick={handleLogout} type="submit" class="menu-item logout">
            Выход
            </button>
        </form>
      </div>
    </div>
  {/if}
</div>

<style>
  .file-menu {
    position: relative;
    z-index: 1000;
  }

  .menu-button {
    background: #4299e1;
    color: white;
    border: none;
    padding: 0.75rem 1.5rem;
    border-radius: 6px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background 0.2s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .menu-button:hover {
    background: #3182ce;
  }

  .dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    margin-top: 0.5rem;
    z-index: 1001;
  }

  .dropdown-content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.1);
    min-width: 220px;
    overflow: visible;
    border: 1px solid #e2e8f0;
    position: relative;
  }

  .menu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    width: 100%;
    padding: 0.875rem 1rem;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
    color: #2d3748;
    transition: background 0.2s;
    position: relative;
  }

  .menu-item.with-submenu {
    position: relative;
  }

  .menu-item:hover {
    background: #f7fafc;
  }

  .menu-item.logout {
    color: #e53e3e;
  }

  hr {
    margin: 0;
    border: none;
    border-top: 1px solid #e2e8f0;
  }
  
  .arrow-icon {
    opacity: 0.6;
    transition: transform 0.2s;
  }
  
  .menu-item.with-submenu:hover .arrow-icon {
    opacity: 1;
  }
  
  .submenu {
    position: absolute;
    top: 0;
    left: 100%;
    margin-left: 2px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.1);
    min-width: 220px;
    border: 1px solid #e2e8f0;
    z-index: 1002;
  }
  
  .submenu-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.875rem 1rem;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
    color: #2d3748;
    transition: background 0.2s;
  }
  
  .submenu-item:hover {
    background: #f7fafc;
  }
  
  .user-info {
      display: flex;
      flex-direction: column;
      padding: 1rem;
      gap: 12px;
  }
  
  .user-details {
      text-align: left;
  }
  
  .user-name {
      font-size: 1rem;
      font-weight: 600;
      color: #2d3748;
      line-height: 1.2;
  }
  
  .user-meta {
      font-size: 0.85rem;
      color: #718096;
      display: flex;
      align-items: center;
      gap: 6px;
      line-height: 1.2;
  }
  
  .username {
      font-weight: 500;
  }
  
  .user-role {
      font-style: italic;
  }
</style>